package com.sahalos.sboard.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sboard_settings")

class SboardPreferences(private val context: Context) {

    companion object {
        val KEY_BOTTOM_PADDING = intPreferencesKey("bottom_padding_dp")
        val KEY_KEYBOARD_HEIGHT = floatPreferencesKey("keyboard_height_ratio")
        val KEY_LONG_PRESS_DELAY = longPreferencesKey("long_press_delay_ms")
        val KEY_LONG_PRESS_SYMBOLS = booleanPreferencesKey("long_press_symbols")
        val KEY_VIBRATION = booleanPreferencesKey("enable_vibration")
        val KEY_THEME_ID = stringPreferencesKey("theme_id")
        val KEY_PASSIVE_SUGGESTIONS = booleanPreferencesKey("passive_suggestions")
        val KEY_KEY_POPUP_PREVIEW = booleanPreferencesKey("enable_key_popup_preview")
        val KEY_SPACEBAR_CURSOR_GLIDE = booleanPreferencesKey("enable_spacebar_cursor_glide")
        val KEY_CLIPBOARD_HISTORY = booleanPreferencesKey("enable_clipboard_history")
        val KEY_RECENT_EMOJIS = stringPreferencesKey("recent_emojis")
    }

    val bottomPaddingDp: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_BOTTOM_PADDING] ?: 0
    }

    val keyboardHeightRatio: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_KEYBOARD_HEIGHT] ?: 1.0f
    }

    val longPressDelayMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LONG_PRESS_DELAY] ?: 300L
    }

    val enableLongPressSymbols: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_LONG_PRESS_SYMBOLS] ?: true
    }

    val enableVibration: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VIBRATION] ?: true
    }

    val themeId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_ID] ?: "nord_dark"
    }

    val enablePassiveSuggestions: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PASSIVE_SUGGESTIONS] ?: true
    }

    val enableKeyPopupPreview: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_KEY_POPUP_PREVIEW] ?: true
    }

    val enableSpacebarCursorGlide: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPACEBAR_CURSOR_GLIDE] ?: true
    }

    val enableClipboardHistory: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CLIPBOARD_HISTORY] ?: true
    }

    val recentEmojis: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_RECENT_EMOJIS] ?: ""
        if (raw.isEmpty()) emptyList() else raw.split("\u0001")
    }

    suspend fun setBottomPaddingDp(dp: Int) {
        context.dataStore.edit { it[KEY_BOTTOM_PADDING] = dp }
    }

    suspend fun setKeyboardHeightRatio(ratio: Float) {
        context.dataStore.edit { it[KEY_KEYBOARD_HEIGHT] = ratio }
    }

    suspend fun setLongPressDelayMs(delay: Long) {
        context.dataStore.edit { it[KEY_LONG_PRESS_DELAY] = delay }
    }

    suspend fun setEnableLongPressSymbols(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LONG_PRESS_SYMBOLS] = enabled }
    }

    suspend fun setEnableVibration(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION] = enabled }
    }

    suspend fun setThemeId(themeId: String) {
        context.dataStore.edit { it[KEY_THEME_ID] = themeId }
    }

    suspend fun setEnablePassiveSuggestions(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PASSIVE_SUGGESTIONS] = enabled }
    }

    suspend fun setEnableKeyPopupPreview(enabled: Boolean) {
        context.dataStore.edit { it[KEY_KEY_POPUP_PREVIEW] = enabled }
    }

    suspend fun setEnableSpacebarCursorGlide(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SPACEBAR_CURSOR_GLIDE] = enabled }
    }

    suspend fun setEnableClipboardHistory(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CLIPBOARD_HISTORY] = enabled }
    }

    suspend fun addRecentEmoji(emoji: String) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_RECENT_EMOJIS] ?: "")
                .split("\u0001")
                .filter { it.isNotEmpty() && it != emoji }
                .toMutableList()
            current.add(0, emoji)
            prefs[KEY_RECENT_EMOJIS] = current.take(36).joinToString("\u0001")
        }
    }
}
