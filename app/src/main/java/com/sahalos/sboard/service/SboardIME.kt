package com.sahalos.sboard.service

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.DefaultEmojiCompatConfig
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sahalos.sboard.MainActivity
import com.sahalos.sboard.R
import com.sahalos.sboard.clipboard.SboardClipboardManager
import com.sahalos.sboard.core.PassiveSuggestionEngine
import com.sahalos.sboard.data.SboardPreferences
import com.sahalos.sboard.model.KeyModel
import com.sahalos.sboard.model.KeyType
import com.sahalos.sboard.model.KeyboardMode
import com.sahalos.sboard.model.ModifierState
import com.sahalos.sboard.model.SboardTheme
import com.sahalos.sboard.core.EmojiCategory
import com.sahalos.sboard.core.EmojiProvider
import com.sahalos.sboard.ui.keyboard.ClipboardScreen
import com.sahalos.sboard.ui.keyboard.EmojiScreen
import com.sahalos.sboard.ui.keyboard.EmojiSearchToolbar
import com.sahalos.sboard.ui.keyboard.ResizeOverlayView
import com.sahalos.sboard.ui.keyboard.SboardKeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SboardIME : InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var preferences: SboardPreferences
    private lateinit var clipboardManager: SboardClipboardManager
    private var vibrator: Vibrator? = null

    // Screen mode (Keyboard, Clipboard, Resize, Emoji, Emoji Search)
    private var keyboardMode by mutableStateOf(KeyboardMode.KEYBOARD)
    private var emojiSearchQuery by mutableStateOf("")

    // Keyboard state
    private var modifierState by mutableStateOf(ModifierState())
    private var currentComposingWord by mutableStateOf("")
    private var currentSuggestions by mutableStateOf<List<String>>(emptyList())

    // Dynamic Action Key
    private var currentActionIcon by mutableStateOf(R.drawable.ic_enter)
    private var currentActionLabel by mutableStateOf<String?>(null)
    private var currentActionId by mutableStateOf(EditorInfo.IME_ACTION_UNSPECIFIED)

    private var enableVibration = true
    private var isPasswordField = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        preferences = SboardPreferences(this)
        clipboardManager = SboardClipboardManager(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        serviceScope.launch {
            preferences.enableVibration.collect { enableVibration = it }
        }

        serviceScope.launch(Dispatchers.IO) {
            EmojiProvider.load(this@SboardIME)
        }

        // Initialize EmojiCompat for system-wide font fallback and modern glyph support
        try {
            val config = DefaultEmojiCompatConfig.create(this)
            if (config != null) {
                EmojiCompat.init(config)
                Log.i("SboardIME", "EmojiCompat initialized successfully")
            }
        } catch (e: Throwable) {
            Log.w("SboardIME", "EmojiCompat init skipped: ${e.message}")
        }

        // Initialize Adaptive User Dictionary and Suggestion Engine
        PassiveSuggestionEngine.init(this)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun installViewTreeOwners() {
        val win = window?.window ?: return
        val decorView = win.decorView

        decorView.setViewTreeLifecycleOwner(this)
        decorView.setViewTreeViewModelStoreOwner(this)
        decorView.setViewTreeSavedStateRegistryOwner(this)

        decorView.findViewById<View>(android.R.id.content)?.let { content ->
            content.setViewTreeLifecycleOwner(this)
            content.setViewTreeViewModelStoreOwner(this)
            content.setViewTreeSavedStateRegistryOwner(this)
        }

        val parentPanelId = resources.getIdentifier("parentPanel", "id", "android")
        if (parentPanelId != 0) {
            decorView.findViewById<View>(parentPanelId)?.let { panel ->
                panel.setViewTreeLifecycleOwner(this)
                panel.setViewTreeViewModelStoreOwner(this)
                panel.setViewTreeSavedStateRegistryOwner(this)
            }
        }

        if (decorView is ViewGroup) {
            for (i in 0 until decorView.childCount) {
                val child = decorView.getChildAt(i)
                child.setViewTreeLifecycleOwner(this)
                child.setViewTreeViewModelStoreOwner(this)
                child.setViewTreeSavedStateRegistryOwner(this)
            }
        }
    }

    override fun onConfigureWindow(win: Window, isFullscreen: Boolean, isCandidatesOnly: Boolean) {
        super.onConfigureWindow(win, isFullscreen, isCandidatesOnly)
        installViewTreeOwners()
    }

    override fun onCreateInputView(): View {
        installViewTreeOwners()

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(this@SboardIME))
            setViewTreeLifecycleOwner(this@SboardIME)
            setViewTreeViewModelStoreOwner(this@SboardIME)
            setViewTreeSavedStateRegistryOwner(this@SboardIME)

            setContent {
                val bottomPadding by preferences.bottomPaddingDp.collectAsStateWithLifecycle(initialValue = 0)
                val heightRatio by preferences.keyboardHeightRatio.collectAsStateWithLifecycle(initialValue = 1.0f)
                val enableLongPressSymbols by preferences.enableLongPressSymbols.collectAsStateWithLifecycle(initialValue = true)
                val enableKeyPopup by preferences.enableKeyPopupPreview.collectAsStateWithLifecycle(initialValue = true)
                val themeId by preferences.themeId.collectAsStateWithLifecycle(initialValue = "nord_dark")
                val clips by clipboardManager.clips.collectAsStateWithLifecycle()
                val recentEmojis by preferences.recentEmojis.collectAsStateWithLifecycle(initialValue = emptyList())

                val activeTheme = SboardTheme.AvailableThemes.find { it.id == themeId } ?: SboardTheme.NordDark

                // Calculate exact keyboard bounds
                val baseRowHeightDp = (48 * heightRatio).toInt()
                val totalKeyboardHeightDp = (42 + (baseRowHeightDp * 4) + bottomPadding).dp

                // Local states for live resize preview
                var previewHeightRatio by remember(heightRatio) { mutableFloatStateOf(heightRatio) }
                var previewBottomPaddingDp by remember(bottomPadding) { mutableIntStateOf(bottomPadding) }
                val previewBaseRowHeightDp = (48 * previewHeightRatio).toInt()
                val previewTotalHeightDp = (42 + (previewBaseRowHeightDp * 4) + previewBottomPaddingDp).dp

                AnimatedContent(
                    targetState = keyboardMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "KeyboardModeTransition"
                ) { mode ->
                    when (mode) {
                        KeyboardMode.KEYBOARD -> {
                            SboardKeyboardView(
                                modifierState = modifierState,
                                theme = activeTheme,
                                suggestions = if (isPasswordField) emptyList() else currentSuggestions,
                                bottomPaddingDp = bottomPadding,
                                keyboardHeightRatio = heightRatio,
                                enableLongPressSymbols = enableLongPressSymbols,
                                enableKeyPopup = enableKeyPopup,
                                actionImeLabel = currentActionLabel,
                                actionIconRes = currentActionIcon,
                                onKeyClick = { key -> handleKeyClick(key) },
                                onKeyLongClick = { key -> handleKeyLongClick(key) },
                                onBackspaceRepeat = { performBackspace() },
                                onCursorMove = { offset -> moveCursor(offset) },
                                onShowImePicker = { showImePicker() },
                                onSuggestionClick = { word -> commitSuggestion(word) },
                                onEmojiClick = { keyboardMode = KeyboardMode.EMOJI },
                                onClipboardClick = { keyboardMode = KeyboardMode.CLIPBOARD },
                                onResizeClick = {
                                    previewHeightRatio = heightRatio
                                    previewBottomPaddingDp = bottomPadding
                                    keyboardMode = KeyboardMode.RESIZE
                                },
                                onSettingsClick = { openSettings() },
                                onCtrlToggle = {
                                    triggerHaptic()
                                    modifierState = modifierState.copy(isCtrlActive = !modifierState.isCtrlActive)
                                },
                                onFnToggle = {
                                    triggerHaptic()
                                    modifierState = modifierState.copy(isFnActive = !modifierState.isFnActive)
                                },
                                onFunctionKeyClick = { keyCode -> sendFunctionKey(keyCode) }
                            )
                        }
                        KeyboardMode.CLIPBOARD -> {
                            ClipboardScreen(
                                clips = clips,
                                theme = activeTheme,
                                totalKeyboardHeight = totalKeyboardHeightDp,
                                onClipClick = { text ->
                                    commitTextDirectly(text)
                                    keyboardMode = KeyboardMode.KEYBOARD
                                },
                                onTogglePin = { clipId -> clipboardManager.togglePin(clipId) },
                                onDeleteClip = { clipId -> clipboardManager.removeClip(clipId) },
                                onClearAll = { clipboardManager.clearAllUnpinned() },
                                onBackToKeyboard = { keyboardMode = KeyboardMode.KEYBOARD }
                            )
                        }
                        KeyboardMode.EMOJI -> {
                            EmojiScreen(
                                theme = activeTheme,
                                recentEmojis = recentEmojis,
                                totalKeyboardHeight = totalKeyboardHeightDp,
                                onEmojiClick = { emoji ->
                                    commitTextDirectly(emoji)
                                    lifecycleScope.launch {
                                        preferences.addRecentEmoji(emoji)
                                    }
                                },
                                onBackspaceClick = { performBackspace() },
                                onBackToKeyboard = { keyboardMode = KeyboardMode.KEYBOARD },
                                onOpenEmojiSearch = {
                                    emojiSearchQuery = ""
                                    keyboardMode = KeyboardMode.EMOJI_SEARCH
                                }
                            )
                        }
                        KeyboardMode.EMOJI_SEARCH -> {
                            val searchResults = remember(emojiSearchQuery) {
                                if (emojiSearchQuery.isBlank()) {
                                    emptyList()
                                } else {
                                    EmojiProvider.search(this@SboardIME, emojiSearchQuery)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(activeTheme.backgroundColor))
                            ) {
                                EmojiSearchToolbar(
                                    query = emojiSearchQuery,
                                    results = searchResults,
                                    theme = activeTheme,
                                    onEmojiClick = { emoji ->
                                        commitTextDirectly(emoji)
                                        lifecycleScope.launch {
                                            preferences.addRecentEmoji(emoji)
                                        }
                                    },
                                    onClearQuery = { emojiSearchQuery = "" },
                                    onCloseSearch = {
                                        emojiSearchQuery = ""
                                        keyboardMode = KeyboardMode.KEYBOARD
                                    },
                                    onQuickFilterClick = { tag ->
                                        emojiSearchQuery = tag
                                    }
                                )

                                SboardKeyboardView(
                                    modifierState = modifierState,
                                    theme = activeTheme,
                                    suggestions = emptyList(),
                                    bottomPaddingDp = bottomPadding,
                                    keyboardHeightRatio = heightRatio,
                                    enableLongPressSymbols = enableLongPressSymbols,
                                    enableKeyPopup = enableKeyPopup,
                                    actionImeLabel = "Selesai",
                                    actionIconRes = R.drawable.ic_enter,
                                    onKeyClick = { key ->
                                        triggerHaptic()
                                        when (key.type) {
                                            KeyType.CHARACTER -> {
                                                emojiSearchQuery += key.primaryLabel
                                            }
                                            KeyType.SPACE -> {
                                                emojiSearchQuery += " "
                                            }
                                            KeyType.BACKSPACE -> {
                                                if (emojiSearchQuery.isNotEmpty()) {
                                                    emojiSearchQuery = emojiSearchQuery.dropLast(1)
                                                } else {
                                                    keyboardMode = KeyboardMode.EMOJI
                                                }
                                            }
                                            KeyType.ACTION -> {
                                                keyboardMode = KeyboardMode.KEYBOARD
                                            }
                                            KeyType.SYM -> {
                                                modifierState = modifierState.copy(isSymActive = !modifierState.isSymActive, isSymPage2Active = false)
                                            }
                                            KeyType.SYM_PAGE_2 -> {
                                                modifierState = modifierState.copy(isSymPage2Active = !modifierState.isSymPage2Active)
                                            }
                                            KeyType.EMOJI -> {
                                                keyboardMode = KeyboardMode.EMOJI
                                            }
                                            else -> {}
                                        }
                                    },
                                    onKeyLongClick = {},
                                    onBackspaceRepeat = {
                                        if (emojiSearchQuery.isNotEmpty()) {
                                            emojiSearchQuery = emojiSearchQuery.dropLast(1)
                                        }
                                    },
                                    onCursorMove = {},
                                    onShowImePicker = { showImePicker() },
                                    onSuggestionClick = {},
                                    onEmojiClick = { keyboardMode = KeyboardMode.EMOJI },
                                    onClipboardClick = { keyboardMode = KeyboardMode.CLIPBOARD },
                                    onResizeClick = {
                                        previewHeightRatio = heightRatio
                                        previewBottomPaddingDp = bottomPadding
                                        keyboardMode = KeyboardMode.RESIZE
                                    },
                                    onSettingsClick = { openSettings() },
                                    onCtrlToggle = {},
                                    onFnToggle = {},
                                    onFunctionKeyClick = {},
                                    showSuggestionsBar = false
                                )
                            }
                        }
                        KeyboardMode.RESIZE -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(previewTotalHeightDp)
                            ) {
                                // Keyboard rendered underneath with preview dimensions
                                SboardKeyboardView(
                                    modifierState = modifierState,
                                    theme = activeTheme,
                                    suggestions = emptyList(),
                                    bottomPaddingDp = previewBottomPaddingDp,
                                    keyboardHeightRatio = previewHeightRatio,
                                    enableLongPressSymbols = enableLongPressSymbols,
                                    enableKeyPopup = false,
                                    actionImeLabel = currentActionLabel,
                                    actionIconRes = currentActionIcon,
                                    onKeyClick = {},
                                    onKeyLongClick = {},
                                    onBackspaceRepeat = {},
                                    onCursorMove = {},
                                    onShowImePicker = {},
                                    onSuggestionClick = {},
                                    onEmojiClick = {},
                                    onClipboardClick = {},
                                    onResizeClick = {},
                                    onSettingsClick = {},
                                    onCtrlToggle = {},
                                    onFnToggle = {},
                                    onFunctionKeyClick = {},
                                    showSuggestionsBar = false
                                )

                                // Interactive Gboard-style Resize Overlay
                                ResizeOverlayView(
                                    currentHeightRatio = previewHeightRatio,
                                    currentBottomPaddingDp = previewBottomPaddingDp,
                                    theme = activeTheme,
                                    onHeightRatioChange = { previewHeightRatio = it },
                                    onBottomPaddingChange = { previewBottomPaddingDp = it },
                                    onReset = {
                                        previewHeightRatio = 1.0f
                                        previewBottomPaddingDp = 0
                                    },
                                    onCancel = { keyboardMode = KeyboardMode.KEYBOARD },
                                    onSave = {
                                        serviceScope.launch {
                                            preferences.setKeyboardHeightRatio(previewHeightRatio)
                                            preferences.setBottomPaddingDp(previewBottomPaddingDp)
                                        }
                                        keyboardMode = KeyboardMode.KEYBOARD
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        installViewTreeOwners()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        currentComposingWord = ""
        currentSuggestions = emptyList()
        keyboardMode = KeyboardMode.KEYBOARD

        // Check if password field
        val variation = (info?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION
        isPasswordField = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

        // Evaluate Dynamic Action Key
        resolveImeAction(info)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        installViewTreeOwners()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        keyboardMode = KeyboardMode.KEYBOARD
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        currentComposingWord = ""
        currentSuggestions = emptyList()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        clipboardManager.destroy()
    }

    private fun resolveImeAction(info: EditorInfo?) {
        val action = (info?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
        currentActionId = action
        when (action) {
            EditorInfo.IME_ACTION_SEARCH -> {
                currentActionIcon = R.drawable.ic_search
                currentActionLabel = "Cari"
            }
            EditorInfo.IME_ACTION_SEND -> {
                currentActionIcon = R.drawable.ic_send
                currentActionLabel = "Kirim"
            }
            EditorInfo.IME_ACTION_GO -> {
                currentActionIcon = R.drawable.ic_next
                currentActionLabel = "Buka"
            }
            EditorInfo.IME_ACTION_NEXT -> {
                currentActionIcon = R.drawable.ic_next
                currentActionLabel = "Lanjut"
            }
            EditorInfo.IME_ACTION_DONE -> {
                currentActionIcon = R.drawable.ic_enter
                currentActionLabel = "Selesai"
            }
            else -> {
                currentActionIcon = R.drawable.ic_enter
                currentActionLabel = null
            }
        }
    }

    private fun commitTextDirectly(text: String) {
        triggerHaptic()
        val ic = currentInputConnection ?: return
        if (currentComposingWord.isNotEmpty()) {
            PassiveSuggestionEngine.learnWord(currentComposingWord)
        }
        ic.commitText(text, 1)
        currentComposingWord = ""
        currentSuggestions = emptyList()
    }

    private fun handleKeyClick(key: KeyModel) {
        triggerHaptic()
        val ic = currentInputConnection ?: return

        when (key.type) {
            KeyType.CHARACTER -> {
                if (modifierState.isCtrlActive) {
                    handleCtrlShortcut(key.primaryLabel)
                    modifierState = modifierState.copy(isCtrlActive = false)
                    return
                }

                val charToCommit = if (modifierState.isShifted || modifierState.isCapsLock) {
                    key.primaryLabel.uppercase()
                } else {
                    key.primaryLabel
                }

                ic.commitText(charToCommit, 1)

                // Update composing word for passive suggestions
                if (!isPasswordField && charToCommit.length == 1 && charToCommit[0].isLetter()) {
                    currentComposingWord += charToCommit
                    currentSuggestions = PassiveSuggestionEngine.getSuggestions(currentComposingWord)
                } else {
                    if (currentComposingWord.isNotEmpty()) {
                        PassiveSuggestionEngine.learnWord(currentComposingWord)
                    }
                    currentComposingWord = ""
                    currentSuggestions = emptyList()
                }

                // Auto reset shift if not caps lock
                if (modifierState.isShifted && !modifierState.isCapsLock) {
                    modifierState = modifierState.copy(isShifted = false)
                }
            }
            KeyType.SPACE -> {
                if (currentComposingWord.isNotEmpty()) {
                    PassiveSuggestionEngine.learnWord(currentComposingWord)
                }
                ic.commitText(" ", 1)
                currentComposingWord = ""
                currentSuggestions = emptyList()
            }
            KeyType.BACKSPACE -> {
                performBackspace()
            }
            KeyType.SHIFT -> {
                if (modifierState.isCapsLock) {
                    modifierState = modifierState.copy(isShifted = false, isCapsLock = false)
                } else if (modifierState.isShifted) {
                    modifierState = modifierState.copy(isCapsLock = true)
                } else {
                    modifierState = modifierState.copy(isShifted = true)
                }
            }
            KeyType.CTRL -> {
                modifierState = modifierState.copy(isCtrlActive = !modifierState.isCtrlActive)
            }
            KeyType.FN -> {
                modifierState = modifierState.copy(isFnActive = !modifierState.isFnActive)
            }
            KeyType.SYM -> {
                val nextSymActive = !modifierState.isSymActive
                modifierState = modifierState.copy(
                    isSymActive = nextSymActive,
                    isSymPage2Active = false
                )
            }
            KeyType.SYM_PAGE_2 -> {
                modifierState = modifierState.copy(
                    isSymPage2Active = !modifierState.isSymPage2Active
                )
            }
            KeyType.EMOJI -> {
                keyboardMode = KeyboardMode.EMOJI
            }
            KeyType.ACTION -> {
                if (currentComposingWord.isNotEmpty()) {
                    PassiveSuggestionEngine.learnWord(currentComposingWord)
                }
                currentComposingWord = ""
                currentSuggestions = emptyList()
                if (currentActionId != EditorInfo.IME_ACTION_UNSPECIFIED && currentActionId != EditorInfo.IME_ACTION_NONE) {
                    ic.performEditorAction(currentActionId)
                } else {
                    sendKeyChar('\n')
                }
            }
            else -> {}
        }
    }

    private fun handleKeyLongClick(key: KeyModel) {
        triggerHaptic()
        val symbol = key.secondaryLabel ?: return
        val ic = currentInputConnection ?: return
        ic.commitText(symbol, 1)
        currentComposingWord = ""
        currentSuggestions = emptyList()
    }

    private fun handleCtrlShortcut(letter: String) {
        val ic = currentInputConnection ?: return
        val lower = letter.lowercase()
        when (lower) {
            "a" -> ic.performContextMenuAction(android.R.id.selectAll)
            "c" -> ic.performContextMenuAction(android.R.id.copy)
            "v" -> ic.performContextMenuAction(android.R.id.paste)
            "x" -> ic.performContextMenuAction(android.R.id.cut)
            "z" -> ic.performContextMenuAction(android.R.id.undo)
            else -> {
                val keyCode = KeyEvent.keyCodeFromString("KEYCODE_${letter.uppercase()}")
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    val eventDown = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_CTRL_ON)
                    val eventUp = KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0, KeyEvent.META_CTRL_ON)
                    ic.sendKeyEvent(eventDown)
                    ic.sendKeyEvent(eventUp)
                }
            }
        }
    }

    private fun performBackspace() {
        triggerHaptic()
        val ic = currentInputConnection ?: return
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            val deleted = ic.deleteSurroundingText(1, 0)
            if (!deleted) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
            }
        }
        if (currentComposingWord.isNotEmpty()) {
            currentComposingWord = currentComposingWord.dropLast(1)
            currentSuggestions = PassiveSuggestionEngine.getSuggestions(currentComposingWord)
        }
    }

    private fun moveCursor(offset: Int) {
        triggerHaptic()
        val keyCode = if (offset > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        val count = kotlin.math.abs(offset)
        for (i in 0 until count) {
            sendDownUpKeyEvents(keyCode)
        }
    }

    private fun commitSuggestion(word: String) {
        triggerHaptic()
        val ic = currentInputConnection ?: return
        if (currentComposingWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentComposingWord.length, 0)
        }
        ic.commitText("$word ", 1)
        PassiveSuggestionEngine.learnWord(word, boost = 5) // Prioritize user-accepted suggestion
        currentComposingWord = ""
        currentSuggestions = emptyList()
    }

    private fun sendFunctionKey(keyCode: Int) {
        triggerHaptic()
        val ic = currentInputConnection ?: return
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        ic.sendKeyEvent(eventDown)
        ic.sendKeyEvent(eventUp)
    }

    private fun showImePicker() {
        triggerHaptic()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showInputMethodPicker()
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    private fun triggerHaptic() {
        if (!enableVibration) return
        vibrator?.let { v ->
            if (v.hasVibrator()) {
                v.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}
