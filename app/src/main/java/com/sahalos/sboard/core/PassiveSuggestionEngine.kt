package com.sahalos.sboard.core

import android.content.Context
import android.util.Log
import com.sahalos.sboard.data.UserDictionaryDbHelper
import com.sahalos.sboard.data.WordRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object PassiveSuggestionEngine {
    private const val TAG = "AdaptiveSuggestionEngine"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var dbHelper: UserDictionaryDbHelper? = null

    // Production-grade Trie data structure for O(L) instant prefix and fuzzy queries
    private val trie = TrieTree()

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
                    isInitialized = true
                    Log.i(TAG, "Adaptive suggestion engine initialized with ${words.size} words in Trie")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load words into Trie: ${e.message}")
                }
            }
        }
    }

    fun learnWord(word: String, boost: Int = 1) {
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
        return trie.searchPrefix(prefix, maxCount = 1, minFrequency = 2).isNotEmpty() ||
            trie.getWordFrequency(prefix) >= 2
    }

    fun getSuggestions(composingWord: String, maxCount: Int = 3): List<String> {
        val trimmed = composingWord.trim()
        if (trimmed.isEmpty()) return emptyList()

        val isFirstCharUpper = trimmed.first().isUpperCase()
        val queryLower = trimmed.lowercase()

        // 1. O(L) instant prefix match from Trie (only words with frequency >= 2 to filter out 1-time typos)
        val prefixRecords = trie.searchPrefix(queryLower, maxCount = maxCount + 1, minFrequency = 2)
        val results = mutableListOf<String>()

        for (cand in prefixRecords) {
            if (cand.word != queryLower) {
                results.add(formatWordCase(cand.word, isFirstCharUpper))
                if (results.size >= maxCount) break
            }
        }

        // 2. Branch-pruned Trie fuzzy search if prefix candidates are fewer than maxCount
        if (results.size < maxCount && queryLower.length >= 2) {
            val needed = maxCount - results.size
            val fuzzyRecords = trie.searchFuzzy(queryLower, maxCount = needed * 3, minFrequency = 2)

            val scoredFuzzy = fuzzyRecords
                .filter { it.word !in results.map { r -> r.lowercase() } && it.word != queryLower }
                .map { rec ->
                    val bigramBonus = if (rec.word.length >= 2) {
                        NativeCoreBridge.getBigramScore(rec.word[0], rec.word[1])
                    } else {
                        0.5f
                    }
                    // Give bonus if first letter matches what the user typed
                    val firstCharBonus = if (rec.word.firstOrNull() == queryLower.firstOrNull()) 15f else 0f
                    val finalScore = (rec.frequency * 1.2f) + (bigramBonus * 10f) + firstCharBonus
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
