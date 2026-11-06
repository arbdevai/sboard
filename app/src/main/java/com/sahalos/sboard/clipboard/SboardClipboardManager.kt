package com.sahalos.sboard.clipboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import com.sahalos.sboard.model.ClipItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class SboardClipboardManager(private val context: Context) {

    companion object {
        private const val TAG = "SboardClipboardManager"
        private const val STORAGE_FILE_NAME = "sboard_clipboard_history.json"
        private const val MAX_UNPINNED_CLIPS = 40
    }

    private val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    private val storageFile = File(context.filesDir, STORAGE_FILE_NAME)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _clips = MutableStateFlow<List<ClipItem>>(emptyList())
    val clips: StateFlow<List<ClipItem>> = _clips.asStateFlow()

    private var lastObservedClipText: String? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        onPrimaryClipChanged()
    }

    init {
        // 1. Non-blocking asynchronous load from disk to prevent UI thread strict mode violations
        ioScope.launch {
            loadFromDisk()
            // Inspect initial system clipboard text without overwriting pinned items
            withContext(Dispatchers.Main) {
                onPrimaryClipChanged()
            }
        }

        // 2. Attach listener to observe future copy actions
        try {
            systemClipboard?.addPrimaryClipChangedListener(clipListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register clipboard listener: ${e.message}")
        }
    }

    fun destroy() {
        try {
            systemClipboard?.removePrimaryClipChangedListener(clipListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister clipboard listener: ${e.message}")
        }
    }

    private fun loadFromDisk() {
        try {
            if (storageFile.exists()) {
                val content = storageFile.readText(Charsets.UTF_8)
                if (content.isNotBlank()) {
                    val loadedClips = json.decodeFromString<List<ClipItem>>(content)
                    // Pinned clips always at the top, followed by unpinned
                    val pinned = loadedClips.filter { it.isPinned }
                    val unpinned = loadedClips.filter { !it.isPinned }.take(MAX_UNPINNED_CLIPS)
                    _clips.value = pinned + unpinned
                    Log.i(TAG, "Loaded ${loadedClips.size} clips from disk (pinned: ${pinned.size})")
                    return
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading clips from disk: ${e.message}")
        }
        _clips.value = emptyList()
    }

    private fun persistToDisk(list: List<ClipItem>) {
        ioScope.launch {
            try {
                val serialized = json.encodeToString(list)
                val tempFile = File(context.filesDir, "$STORAGE_FILE_NAME.tmp")
                tempFile.writeText(serialized, Charsets.UTF_8)
                if (tempFile.exists()) {
                    tempFile.renameTo(storageFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving clips to disk: ${e.message}")
            }
        }
    }

    private fun onPrimaryClipChanged() {
        val clipboard = systemClipboard ?: return
        if (!clipboard.hasPrimaryClip()) return

        val clip = clipboard.primaryClip ?: return
        val description = clip.description

        // Check if sensitive (e.g. password fields, EXTRA_IS_SENSITIVE)
        val isSensitive = description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true &&
                (description.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true)

        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim() ?: return
            if (text.isEmpty() || text == lastObservedClipText) return
            lastObservedClipText = text

            addClip(text, isPinned = false, isSensitive = isSensitive)
        }
    }

    fun addClip(text: String, isPinned: Boolean = false, isSensitive: Boolean = false) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        val current = _clips.value.toMutableList()

        // If this text is ALREADY PINNED, preserve its pinned status and do not duplicate
        val existingPinned = current.find { it.text == cleanText && it.isPinned }
        if (existingPinned != null && !isPinned) {
            // Already safely pinned, keep it as pinned
            return
        }

        // Remove any existing unpinned duplicate of the same text
        current.removeAll { it.text == cleanText && !it.isPinned }

        val newClip = ClipItem(
            id = UUID.randomUUID().toString(),
            text = cleanText,
            timestamp = System.currentTimeMillis(),
            isPinned = isPinned,
            isSensitive = isSensitive
        )

        // Split into pinned and unpinned
        val pinned = (if (isPinned) listOf(newClip) + current.filter { it.isPinned } else current.filter { it.isPinned })
            .distinctBy { it.id }
        val unpinned = (if (!isPinned) listOf(newClip) + current.filter { !it.isPinned } else current.filter { !it.isPinned })
            .distinctBy { it.id }

        val trimmedUnpinned = if (unpinned.size > MAX_UNPINNED_CLIPS) {
            unpinned.take(MAX_UNPINNED_CLIPS)
        } else {
            unpinned
        }

        val updated = pinned + trimmedUnpinned
        _clips.value = updated
        persistToDisk(updated)
    }

    fun togglePin(id: String) {
        val current = _clips.value.map { item ->
            if (item.id == id) {
                item.copy(isPinned = !item.isPinned)
            } else {
                item
            }
        }

        // Re-partition so all pinned items stay neatly on top
        val pinned = current.filter { it.isPinned }
        val unpinned = current.filter { !it.isPinned }

        val updated = pinned + unpinned
        _clips.value = updated
        persistToDisk(updated)
    }

    fun removeClip(id: String) {
        val updated = _clips.value.filter { it.id != id }
        _clips.value = updated
        persistToDisk(updated)
    }

    fun clearAllUnpinned() {
        // Keeps 100% of pinned clips, removes only unpinned
        val updated = _clips.value.filter { it.isPinned }
        _clips.value = updated
        persistToDisk(updated)
    }
}
