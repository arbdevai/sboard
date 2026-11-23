package com.sahalos.sboard.core

import android.content.Context
import android.util.Log
import com.sahalos.sboard.data.CustomShortcut
import com.sahalos.sboard.data.UserDictionaryDbHelper
import com.sahalos.sboard.data.WordRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object PassiveSuggestionEngine {
    private const val TAG = "AdaptiveSuggestionEngine"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dbHelper: UserDictionaryDbHelper? = null

    // Production-grade Trie data structure for O(L) instant prefix and fuzzy queries
    private val trie = TrieTree()

    // In-memory cache for instant text expansion shortcut lookups
    private val shortcutsMap = ConcurrentHashMap<String, String>()

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized && dbHelper != null) return

        synchronized(this) {
            if (isInitialized && dbHelper != null) return
            val helper = UserDictionaryDbHelper(context.applicationContext)
            dbHelper = helper

            scope.launch {
                try {
                    val words = helper.loadAllActiveWords(limit = 4000)
                    for (w in words) {
                        trie.insert(w, accumulate = false)
                    }
                    val shortcuts = helper.getAllShortcuts()
                    shortcutsMap.clear()
                    for (s in shortcuts) {
                        shortcutsMap[s.shortcut.lowercase()] = s.expansion
                    }
                    isInitialized = true
                    Log.i(TAG, "Adaptive suggestion engine initialized with ${words.size} words and ${shortcuts.size} shortcuts")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load words into Trie: ${e.message}")
                }
            }
        }
    }

    fun reloadShortcuts() {
        scope.launch {
            val list = dbHelper?.getAllShortcuts() ?: emptyList()
            shortcutsMap.clear()
            for (item in list) {
                shortcutsMap[item.shortcut.lowercase()] = item.expansion
            }
        }
    }

    fun getShortcutExpansion(shortcut: String): String? {
        val clean = shortcut.trim().lowercase()
        return shortcutsMap[clean] ?: dbHelper?.getShortcutExpansion(clean)
    }

    fun getAllShortcuts(): List<CustomShortcut> {
        return dbHelper?.getAllShortcuts() ?: emptyList()
    }

    fun addOrUpdateShortcut(shortcut: String, expansion: String) {
        dbHelper?.addOrUpdateShortcut(shortcut, expansion)
        shortcutsMap[shortcut.trim().lowercase()] = expansion.trim()
    }

    fun deleteShortcut(shortcut: String) {
        dbHelper?.deleteShortcut(shortcut)
        shortcutsMap.remove(shortcut.trim().lowercase())
    }

    fun learnWord(word: String, boost: Int = 3) {
        val clean = word.trim().lowercase()
        if (clean.length < 2 || clean.any { !it.isLetter() }) return

        val now = System.currentTimeMillis()
        val record = WordRecord(word = clean, frequency = boost, lastUsed = now)
        trie.insert(record, accumulate = true)

        // Persist to SQLite asynchronously in background
        scope.launch {
            try {
                dbHelper?.learnWord(clean, boost)
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting learned word '$clean': ${e.message}")
            }
        }
    }

    fun hasPrefixMatch(prefix: String): Boolean {
        if (prefix.isEmpty()) return false
        return trie.searchPrefix(prefix, maxCount = 1, minFrequency = 1).isNotEmpty() ||
            trie.getWordFrequency(prefix) >= 1
    }

    fun getAutocorrectCandidate(composingWord: String): String? {
        val clean = composingWord.trim().lowercase()
        // 1. Never autocorrect short words (<= 3 chars, e.g. "di", "ke", "ya", "ga", "sh", "cd")
        if (clean.length < 4) return null

        // 2. If word is already a known valid word in dictionary, NEVER autocorrect!
        if (trie.getWordFrequency(clean) >= 1) return null

        // 3. Search fuzzy match candidates
        val fuzzyRecords = trie.searchFuzzy(clean, maxCount = 5, minFrequency = 2)
        if (fuzzyRecords.isEmpty()) return null

        // 4. Critical sanity checks for typo replacement:
        // - Must start with the same first letter (typos rarely change the first letter!)
        // - Length difference must be at most 1 char
        // - Candidate must have strong frequency in dictionary (>= 3)
        val best = fuzzyRecords.firstOrNull { cand ->
            cand.word.first() == clean.first() &&
            kotlin.math.abs(cand.word.length - clean.length) <= 1 &&
            cand.frequency >= 3
        } ?: return null

        return formatWordCase(best.word, composingWord.first().isUpperCase())
    }

    fun getSuggestions(composingWord: String, maxCount: Int = 3): List<String> {
        val trimmed = composingWord.trim()
        if (trimmed.isEmpty()) return emptyList()

        val isFirstCharUpper = trimmed.first().isUpperCase()
        val queryLower = trimmed.lowercase()
        val results = mutableListOf<String>()

        // 1. Check custom text shortcuts first
        val exactExpansion = shortcutsMap[queryLower]
        if (exactExpansion != null) {
            results.add(exactExpansion)
        } else {
            val matchingShortcut = shortcutsMap.entries.firstOrNull { it.key.startsWith(queryLower) }
            if (matchingShortcut != null) {
                results.add(matchingShortcut.value)
            }
        }

        // 2. O(L) instant prefix match from Trie (minFrequency = 1 so newly typed words appear immediately)
        val prefixRecords = trie.searchPrefix(queryLower, maxCount = maxCount + 2, minFrequency = 1)
        val now = System.currentTimeMillis()

        // Score prefix candidates by frequency + recency bonus (words used in last 48 hours get boosted)
        val scoredPrefix = prefixRecords
            .filter { it.word != queryLower && it.word !in results.map { r -> r.lowercase() } }
            .map { rec ->
                val hoursSinceUsed = ((now - rec.lastUsed) / (1000 * 3600)).coerceAtLeast(0)
                val recencyBonus = if (hoursSinceUsed < 48) (48 - hoursSinceUsed) / 3f else 0f
                val finalScore = (rec.frequency * 1.5f) + recencyBonus
                Pair(rec, finalScore)
            }
            .sortedByDescending { it.second }

        for (pair in scoredPrefix) {
            results.add(formatWordCase(pair.first.word, isFirstCharUpper))
            if (results.size >= maxCount) break
        }

        // 3. Branch-pruned Trie fuzzy search if prefix candidates are fewer than maxCount
        if (results.size < maxCount && queryLower.length >= 2) {
            val needed = maxCount - results.size
            val fuzzyRecords = trie.searchFuzzy(queryLower, maxCount = needed * 3, minFrequency = 1)

            val scoredFuzzy = fuzzyRecords
                .filter { it.word !in results.map { r -> r.lowercase() } && it.word != queryLower }
                .map { rec ->
                    val bigramBonus = if (rec.word.length >= 2) {
                        NativeCoreBridge.getBigramScore(rec.word[0], rec.word[1])
                    } else {
                        0.5f
                    }
                    val firstCharBonus = if (rec.word.firstOrNull() == queryLower.firstOrNull()) 15f else 0f
                    val hoursSinceUsed = ((now - rec.lastUsed) / (1000 * 3600)).coerceAtLeast(0)
                    val recencyBonus = if (hoursSinceUsed < 48) (48 - hoursSinceUsed) / 3f else 0f
                    val finalScore = (rec.frequency * 1.2f) + (bigramBonus * 10f) + firstCharBonus + recencyBonus
                    Pair(rec, finalScore)
                }
                .sortedByDescending { it.second }

            for (pair in scoredFuzzy.take(needed)) {
                val formatted = formatWordCase(pair.first.word, isFirstCharUpper)
                if (formatted !in results) {
                    results.add(formatted)
                }
            }
        }

        return results.take(maxCount)
    }

    private fun formatWordCase(word: String, isFirstCharUpper: Boolean): String {
        return if (isFirstCharUpper) {
            word.replaceFirstChar { it.uppercase() }
        } else {
            word
        }
    }
}
