package com.sahalos.sboard.core

object PassiveSuggestionEngine {

    private val commonVocabulary = listOf(
        "the", "be", "to", "of", "and", "a", "in", "that", "have", "i",
        "it", "for", "not", "on", "with", "he", "as", "you", "do", "at",
        "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
        "or", "an", "will", "my", "one", "all", "would", "there", "their", "what",
        "so", "up", "out", "if", "about", "who", "get", "which", "go", "me",
        "when", "make", "can", "like", "time", "no", "just", "him", "know", "take",
        "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
        "than", "then", "now", "look", "only", "come", "its", "over", "think", "also",
        "back", "after", "use", "two", "how", "our", "work", "first", "well", "way",
        "even", "new", "want", "because", "any", "these", "give", "day", "most", "us",
        "android", "kotlin", "compose", "keyboard", "mobile", "sboard", "system", "code",
        "developer", "function", "screen", "button", "commit", "release", "branch", "project"
    )

    fun getSuggestions(composingWord: String, maxCount: Int = 3): List<String> {
        val trimmed = composingWord.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        val results = mutableListOf<String>()

        // 1. Exact match / prefix matches
        for (word in commonVocabulary) {
            if (word.startsWith(trimmed) && word != trimmed) {
                results.add(word)
                if (results.size >= maxCount) return results
            }
        }

        // 2. Levenshtein / Edit distance 1 candidates if prefix results are few
        if (results.size < maxCount && trimmed.length >= 3) {
            for (word in commonVocabulary) {
                if (word !in results && Math.abs(word.length - trimmed.length) <= 1) {
                    if (isEditDistanceOne(trimmed, word)) {
                        results.add(word)
                        if (results.size >= maxCount) return results
                    }
                }
            }
        }

        return results
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
        return true
    }
}
