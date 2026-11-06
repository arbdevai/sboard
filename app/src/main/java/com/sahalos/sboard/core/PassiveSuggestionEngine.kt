package com.sahalos.sboard.core

import android.content.Context
import android.util.Log
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

    // In-memory hot cache for instant (< 1ms) prefix lookups during fast typing
    private val memoryWords = ConcurrentHashMap<String, WordRecord>()
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
                        memoryWords[w.word] = w
                    }
                    isInitialized = true
                    Log.i(TAG, "Adaptive suggestion engine initialized with ${memoryWords.size} words in memory")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load words into memory cache: ${e.message}")
                }
            }
        }
    }

    fun learnWord(word: String, boost: Int = 1) {
        val clean = word.trim().lowercase()
        if (clean.length < 2 || clean.any { !it.isLetter() }) return

        val now = System.currentTimeMillis()
        val existing = memoryWords[clean]
        val newRecord = if (existing != null) {
            existing.copy(frequency = existing.frequency + boost, lastUsed = now)
        } else {
            WordRecord(word = clean, frequency = boost, lastUsed = now)
        }
        memoryWords[clean] = newRecord

        // Persist to SQLite asynchronously in background
        scope.launch {
            try {
                dbHelper?.learnWord(clean, boost)
            } catch (e: Exception) {
                Log.e(TAG, "Error persisting learned word '$clean': ${e.message}")
            }
        }
    }

    fun getSuggestions(composingWord: String, maxCount: Int = 3): List<String> {
        val trimmed = composingWord.trim()
        if (trimmed.isEmpty()) return emptyList()

        val isFirstCharUpper = trimmed.first().isUpperCase()
        val queryLower = trimmed.lowercase()

        val prefixCandidates = mutableListOf<WordRecord>()

        // 1. In-memory fast prefix scan
        for ((_, record) in memoryWords) {
            if (record.word.startsWith(queryLower) && record.word != queryLower) {
                prefixCandidates.add(record)
            }
        }

        // Rank by frequency descending, then lastUsed descending
        prefixCandidates.sortWith(
            compareByDescending<WordRecord> { it.frequency }
                .thenByDescending { it.lastUsed }
        )

        val results = mutableListOf<String>()
        for (cand in prefixCandidates.take(maxCount)) {
            results.add(formatWordCase(cand.word, isFirstCharUpper))
        }

        // 2. Typo correction if prefix results are insufficient and query has length >= 3
        if (results.size < maxCount && queryLower.length >= 3) {
            val typoCandidates = mutableListOf<Pair<WordRecord, Float>>()

            for ((_, record) in memoryWords) {
                val candidateWord = record.word
                if (candidateWord in results.map { it.lowercase() } || candidateWord == queryLower) continue

                if (Math.abs(candidateWord.length - queryLower.length) <= 1 && isEditDistanceOne(queryLower, candidateWord)) {
                    // Score with Rust Native Core bigram transition if available
                    val bigramBonus = if (candidateWord.length >= 2 && queryLower.length >= 2) {
                        NativeCoreBridge.getBigramScore(candidateWord[0], candidateWord[1])
                    } else {
                        0.5f
                    }
                    val finalScore = (record.frequency * 1.0f) + (bigramBonus * 10f)
                    typoCandidates.add(Pair(record, finalScore))
                }
            }

            typoCandidates.sortByDescending { it.second }

            for (pair in typoCandidates) {
                val formatted = formatWordCase(pair.first.word, isFirstCharUpper)
                if (formatted !in results) {
                    results.add(formatted)
                    if (results.size >= maxCount) break
                }
            }
        }

        return results
    }

    private fun formatWordCase(word: String, isFirstCharUpper: Boolean): String {
        return if (isFirstCharUpper) {
            word.replaceFirstChar { it.uppercase() }
        } else {
            word
        }
    }

    private fun isEditDistanceOne(s1: String, s2: String): Boolean {
        var diff = 0
        var i = 0
        var j = 0
        while (i < s1.length && j < s2.length) {
            if (s1[i] != s2[j]) {
                diff++
                if (diff > 1) return false
                if (s1.length > s2.length) {
                    i++
                    continue
                } else if (s2.length > s1.length) {
                    j++
                    continue
                }
            }
            i++
            j++
        }
        if (i < s1.length || j < s2.length) {
            diff++
        }
        return diff == 1
    }
}
