package com.sahalos.sboard.model

enum class KeyType {
    CHARACTER,
    SPACE,
    BACKSPACE,
    SHIFT,
    CTRL,
    FN,
    SYM,
    ACTION,
    CLIPBOARD,
    SETTINGS,
    SPACER,
    EMOJI,
    SYM_PAGE_2
}

data class KeyModel(
    val code: Int,
    val primaryLabel: String,
    val secondaryLabel: String? = null,
    val weight: Float = 1.0f,
    val type: KeyType = KeyType.CHARACTER
)

data class ModifierState(
    val isShifted: Boolean = false,
    val isCapsLock: Boolean = false,
    val isCtrlActive: Boolean = false,
    val isAltActive: Boolean = false,
    val isFnActive: Boolean = false,
    val isSymActive: Boolean = false,
    val isSymPage2Active: Boolean = false
)
