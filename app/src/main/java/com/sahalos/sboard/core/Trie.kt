package com.sahalos.sboard.core

import com.sahalos.sboard.data.WordRecord
import java.util.PriorityQueue

class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var record: WordRecord? = null
    var isWord: Boolean = false
    var maxSubtreeFrequency: Int = 0
}

class TrieTree {
    private val root = TrieNode()

    @Synchronized
    fun insert(record: WordRecord) {
        val word = record.word.lowercase()
        var current = root
        current.maxSubtreeFrequency = maxOf(current.maxSubtreeFrequency, record.frequency)

        for (ch in word) {
            current = current.children.getOrPut(ch) { TrieNode() }
            current.maxSubtreeFrequency = maxOf(current.maxSubtreeFrequency, record.frequency)
        }
        current.isWord = true
        current.record = record
    }

    @Synchronized
    fun searchPrefix(prefix: String, maxCount: Int = 3): List<WordRecord> {
        val cleanPrefix = prefix.lowercase()
        var current = root

        for (ch in cleanPrefix) {
            current = current.children[ch] ?: return emptyList()
        }

        val results = mutableListOf<WordRecord>()
        collectWords(current, results)

        results.sortWith(
            compareByDescending<WordRecord> { it.frequency }
                .thenByDescending { it.lastUsed }
        )

        return results.take(maxCount)
    }

    private fun collectWords(node: TrieNode, collector: MutableList<WordRecord>) {
        if (node.isWord) {
            node.record?.let { collector.add(it) }
        }
        for ((_, child) in node.children) {
            collectWords(child, collector)
        }
    }

    @Synchronized
    fun searchFuzzy(query: String, maxCount: Int = 3): List<WordRecord> {
        val cleanQuery = query.lowercase()
        if (cleanQuery.length < 3) return emptyList()

        val results = mutableListOf<WordRecord>()
        val currentDistances = IntArray(cleanQuery.length + 1) { it }

        for ((ch, child) in root.children) {
            fuzzySearchRecursive(
                node = child,
                char = ch,
                wordSoFar = ch.toString(),
                query = cleanQuery,
                prevDistances = currentDistances,
                results = results
            )
        }

        results.sortWith(
            compareByDescending<WordRecord> { it.frequency }
                .thenByDescending { it.lastUsed }
        )

        return results.distinctBy { it.word }.take(maxCount)
    }

    private fun fuzzySearchRecursive(
        node: TrieNode,
        char: Char,
        wordSoFar: String,
        query: String,
        prevDistances: IntArray,
        results: MutableList<WordRecord>
    ) {
        val size = prevDistances.size
        val currentDistances = IntArray(size)
        currentDistances[0] = prevDistances[0] + 1

        var minDistance = currentDistances[0]

        for (i in 1 until size) {
            val cost = if (query[i - 1] == char) 0 else 1
            currentDistances[i] = minOf(
                currentDistances[i - 1] + 1,       // Insertion
                prevDistances[i] + 1,              // Deletion
                prevDistances[i - 1] + cost        // Substitution
            )
            minDistance = minOf(minDistance, currentDistances[i])
        }

        // Branch pruning: if minimum distance in this row > 1, no descendants can match within distance 1
        if (minDistance > 1) return

        if (currentDistances[size - 1] <= 1 && node.isWord && wordSoFar != query) {
            node.record?.let { results.add(it) }
        }

        for ((nextChar, nextNode) in node.children) {
            fuzzySearchRecursive(
                node = nextNode,
                char = nextChar,
                wordSoFar = wordSoFar + nextChar,
                query = query,
                prevDistances = currentDistances,
                results = results
            )
        }
    }

    @Synchronized
    fun clear() {
        root.children.clear()
        root.record = null
        root.isWord = false
        root.maxSubtreeFrequency = 0
    }
}
