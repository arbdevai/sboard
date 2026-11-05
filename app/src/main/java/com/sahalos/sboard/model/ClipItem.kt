package com.sahalos.sboard.model

import kotlinx.serialization.Serializable

@Serializable
data class ClipItem(
    val id: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isSensitive: Boolean = false
)
