package com.sahalos.sboard.clipboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import com.sahalos.sboard.model.ClipItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SboardClipboardManager(private val context: Context) {

    private val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    private val _clips = MutableStateFlow<List<ClipItem>>(emptyList())
    val clips: StateFlow<List<ClipItem>> = _clips.asStateFlow()

    private val maxUnpinnedClips = 40
    private var lastObservedClipText: String? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        onPrimaryClipChanged()
    }

    init {
        systemClipboard?.addPrimaryClipChangedListener(clipListener)
        // Read initial clip on-demand
        onPrimaryClipChanged()
    }

    fun destroy() {
        systemClipboard?.removePrimaryClipChangedListener(clipListener)
    }

    private fun onPrimaryClipChanged() {
        val clipboard = systemClipboard ?: return
        if (!clipboard.hasPrimaryClip()) return

        val clip = clipboard.primaryClip ?: return
        val description = clip.description

        // Check if sensitive (password fields, EXTRA_IS_SENSITIVE)
        val isSensitive = description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true &&
                (description.extras?.getBoolean("android.content.extra.IS_SENSITIVE") == true)

        if (clip.itemCount > 0) {
            val text = clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim() ?: return
            if (text.isEmpty() || text == lastObservedClipText) return
            lastObservedClipText = text

            addClip(text, isSensitive = isSensitive)
        }
    }

    fun addClip(text: String, isPinned: Boolean = false, isSensitive: Boolean = false) {
        val current = _clips.value.toMutableList()

        // Remove if duplicate unpinned exists
        current.removeAll { it.text == text && !it.isPinned }

        val newClip = ClipItem(
            id = UUID.randomUUID().toString(),
            text = text,
            isPinned = isPinned,
            isSensitive = isSensitive
        )
        current.add(0, newClip)

        // FIFO eviction on unpinned clips
        val pinned = current.filter { it.isPinned }
        val unpinned = current.filter { !it.isPinned }

        val trimmedUnpinned = if (unpinned.size > maxUnpinnedClips) {
            unpinned.take(maxUnpinnedClips)
        } else {
            unpinned
        }

        _clips.value = pinned + trimmedUnpinned
    }

    fun togglePin(id: String) {
        _clips.value = _clips.value.map { item ->
            if (item.id == id) item.copy(isPinned = !item.isPinned) else item
        }
    }

    fun removeClip(id: String) {
        _clips.value = _clips.value.filter { it.id != id }
    }

    fun clearAllUnpinned() {
        _clips.value = _clips.value.filter { it.isPinned }
    }
}
