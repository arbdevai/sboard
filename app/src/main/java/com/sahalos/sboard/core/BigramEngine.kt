package com.sahalos.sboard.core

object BigramEngine {

    // Common English / general bigram transition weights (log frequency approximation)
    private val commonBigrams = mapOf(
        "th" to 3.5f, "he" to 3.0f, "in" to 2.8f, "er" to 2.7f, "an" to 2.6f,
        "re" to 2.5f, "on" to 2.4f, "at" to 2.3f, "en" to 2.2f, "nd" to 2.1f,
        "ti" to 2.0f, "es" to 2.0f, "or" to 1.9f, "te" to 1.9f, "of" to 1.8f,
        "ed" to 1.8f, "is" to 1.8f, "it" to 1.7f, "al" to 1.7f, "ar" to 1.6f,
        "st" to 1.6f, "to" to 1.5f, "nt" to 1.5f, "ng" to 1.5f, "se" to 1.4f,
        "ha" to 1.4f, "as" to 1.4f, "ou" to 1.3f, "io" to 1.3f, "le" to 1.3f,
        "ve" to 1.2f, "co" to 1.2f, "me" to 1.2f, "de" to 1.2f, "hi" to 1.1f,
        "ri" to 1.1f, "ro" to 1.1f, "ic" to 1.0f, "ne" to 1.0f, "ea" to 1.0f,
        "ra" to 1.0f, "ce" to 0.9f, "li" to 0.9f, "ch" to 0.9f, "ll" to 0.9f,
        "be" to 0.8f, "ma" to 0.8f, "si" to 0.8f, "om" to 0.8f, "ur" to 0.8f
    )

    fun getTransitionBonus(prevChar: Char?, candidateChar: Char): Float {
        if (prevChar == null) return 0f
        val pair = "${prevChar.lowercaseChar()}${candidateChar.lowercaseChar()}"
        return commonBigrams[pair] ?: 0f
    }
}
