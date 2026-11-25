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
import com.sahalos.sboard.model.SelectionHandle
import com.sahalos.sboard.core.EmojiCategory
import com.sahalos.sboard.core.EmojiProvider
import com.sahalos.sboard.ui.keyboard.ClipboardScreen
import com.sahalos.sboard.ui.keyboard.EmojiScreen
import com.sahalos.sboard.ui.keyboard.EmojiSearchToolbar
import com.sahalos.sboard.ui.keyboard.ResizeOverlayView
import com.sahalos.sboard.ui.keyboard.SboardKeyboardView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    private var lastModeSwitchTimeMs = 0L

    private fun switchToKeyboardMode() {
        lastModeSwitchTimeMs = android.os.SystemClock.uptimeMillis()
        keyboardMode = KeyboardMode.KEYBOARD
    }

    // Keyboard state
    private var modifierState by mutableStateOf(ModifierState())
    private var hasActiveSelection by mutableStateOf(false)
    private var currentComposingWord by mutableStateOf("")
    private var currentSuggestions by mutableStateOf<List<String>>(emptyList())
    private var lastCtrlTapTimeMs = 0L
    private var lastShiftTapTimeMs = 0L
    private var lastAltTapTimeMs = 0L

    private fun toggleCtrl() {
        triggerHaptic()
        val now = android.os.SystemClock.uptimeMillis()
        if (modifierState.isCtrlLocked) {
            modifierState = modifierState.copy(isCtrlActive = false, isCtrlLocked = false)
        } else if (modifierState.isCtrlActive) {
            if (now - lastCtrlTapTimeMs < 450L) {
                modifierState = modifierState.copy(isCtrlActive = true, isCtrlLocked = true)
            } else {
                modifierState = modifierState.copy(isCtrlActive = false, isCtrlLocked = false)
            }
        } else {
            modifierState = modifierState.copy(isCtrlActive = true, isCtrlLocked = false)
        }
        lastCtrlTapTimeMs = now
    }

    private fun toggleShift() {
        triggerHaptic()
        val now = android.os.SystemClock.uptimeMillis()
        if (modifierState.isCapsLock) {
            modifierState = modifierState.copy(isShifted = false, isCapsLock = false)
        } else if (modifierState.isShifted) {
            if (now - lastShiftTapTimeMs < 450L) {
                modifierState = modifierState.copy(isShifted = true, isCapsLock = true)
            } else {
                modifierState = modifierState.copy(isShifted = false, isCapsLock = false)
            }
        } else {
            modifierState = modifierState.copy(isShifted = true, isCapsLock = false)
        }
        lastShiftTapTimeMs = now
    }

    private var activeSelectionHandle by mutableStateOf(SelectionHandle.END)
    private var selectionStartPos by mutableIntStateOf(0)
    private var selectionEndPos by mutableIntStateOf(0)

    private fun deselectText() {
        triggerHaptic()
        val ic = currentInputConnection ?: return
        val pos = selectionEndPos
        ic.setSelection(pos, pos)
        selectionStartPos = pos
        selectionEndPos = pos
        hasActiveSelection = false
        if (modifierState.isShifted && !modifierState.isCapsLock) {
            modifierState = modifierState.copy(isShifted = false)
        }
    }

    // Dynamic Action Key
    private var currentActionIcon by mutableStateOf(R.drawable.ic_enter)
    private var currentActionLabel by mutableStateOf<String?>(null)
    private var currentActionId by mutableStateOf(EditorInfo.IME_ACTION_UNSPECIFIED)

    private var enableVibration = true
    private var enablePassiveSuggestions = true
    private var isPasswordField = false
    private var isTerminalField = false
    private var isEmailField = false
    private var isUriField = false
    private var savedEmailsList: List<String> = emptyList()
    private val commonEmailDomains = listOf("gmail.com", "yahoo.com", "outlook.com", "hotmail.com", "icloud.com")
    private val commonUriSuffixes = listOf(".com", ".id", ".co.id", ".org", ".net", ".io")
    private var isNumberPadField by mutableStateOf(false)
    private var isPhonePadField by mutableStateOf(false)
    private var isDecimalNumberField by mutableStateOf(false)
    private var isSignedNumberField by mutableStateOf(false)
    private var isNumericOverriddenToAlpha by mutableStateOf(false)
    private var backspaceRepeatJob: Job? = null

    private fun startBackspaceRepeat() {
        stopBackspaceRepeat()
        backspaceRepeatJob = serviceScope.launch {
            delay(170L)
            while (isActive) {
                performBackspace()
                delay(28L)
            }
        }
    }

    private fun stopBackspaceRepeat() {
        backspaceRepeatJob?.cancel()
        backspaceRepeatJob = null
    }

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

        serviceScope.launch {
            preferences.enablePassiveSuggestions.collect { enablePassiveSuggestions = it }
        }

        serviceScope.launch {
            preferences.savedEmails.collect { savedEmailsList = it }
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
                val enableNumberRow by preferences.enableNumberRow.collectAsStateWithLifecycle(initialValue = true)
                val themeId by preferences.themeId.collectAsStateWithLifecycle(initialValue = "oled_black")
                val clips by clipboardManager.clips.collectAsStateWithLifecycle()
                val recentEmojis by preferences.recentEmojis.collectAsStateWithLifecycle(initialValue = emptyList())

                val activeTheme = SboardTheme.AvailableThemes.find { it.id == themeId } ?: SboardTheme.OledBlack

                // Calculate exact keyboard bounds with locked constant container height (No layer size jumping)
                val has5RowsConfigured = enableNumberRow && !isNumberPadField && !isPhonePadField
                val contentAreaHeightDp = if (has5RowsConfigured) (210 * heightRatio).toInt() else (192 * heightRatio).toInt()
                val totalKeyboardHeightDp = (42 + contentAreaHeightDp + bottomPadding).dp

                // Local states for live resize preview
                var previewHeightRatio by remember(heightRatio) { mutableFloatStateOf(heightRatio) }
                var previewBottomPaddingDp by remember(bottomPadding) { mutableIntStateOf(bottomPadding) }
                val previewContentAreaHeightDp = if (has5RowsConfigured) (210 * previewHeightRatio).toInt() else (192 * previewHeightRatio).toInt()
                val previewTotalHeightDp = (42 + previewContentAreaHeightDp + previewBottomPaddingDp).dp

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
                                suggestions = if (!enablePassiveSuggestions || isPasswordField || isNumberPadField || isPhonePadField) emptyList() else currentSuggestions,
                                composingWord = currentComposingWord,
                                bottomPaddingDp = bottomPadding,
                                keyboardHeightRatio = heightRatio,
                                enableLongPressSymbols = enableLongPressSymbols,
                                enableKeyPopup = enableKeyPopup,
                                enableNumberRow = enableNumberRow,
                                isNumberPad = isNumberPadField,
                                isPhonePad = isPhonePadField,
                                isDecimalNumber = isDecimalNumberField,
                                isSignedNumber = isSignedNumberField,
                                isNumericOverriddenToAlpha = isNumericOverriddenToAlpha,
                                actionImeLabel = currentActionLabel,
                                actionIconRes = currentActionIcon,
                                onKeyClick = { key -> handleKeyClick(key) },
                                onKeyLongClick = { key -> handleKeyLongClick(key) },
                                onBackspaceRepeat = { performBackspace() },
                                onBackspaceRepeatStart = { startBackspaceRepeat() },
                                onBackspaceRepeatStop = { stopBackspaceRepeat() },
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
                                    if (android.os.SystemClock.uptimeMillis() - lastModeSwitchTimeMs > 350L) {
                                        toggleCtrl()
                                    }
                                },
                                onAltToggle = {
                                    triggerHaptic()
                                    val next = !modifierState.isAltActive
                                    modifierState = modifierState.copy(isAltActive = next, isAltLocked = false)
                                },
                                onShiftToggle = {
                                    toggleShift()
                                },
                                hasSelection = hasActiveSelection,
                                activeSelectionHandle = activeSelectionHandle,
                                onSelectHandleChange = { handle ->
                                    triggerHaptic()
                                    activeSelectionHandle = handle
                                },
                                onDeselectClick = { deselectText() },
                                onFnToggle = {
                                    if (android.os.SystemClock.uptimeMillis() - lastModeSwitchTimeMs > 350L) {
                                        triggerHaptic()
                                        val nextFn = !modifierState.isFnActive
                                        modifierState = modifierState.copy(
                                            isFnActive = nextFn,
                                            isAltActive = if (!nextFn) false else modifierState.isAltActive
                                        )
                                    }
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
                                    switchToKeyboardMode()
                                },
                                onTogglePin = { clipId -> clipboardManager.togglePin(clipId) },
                                onDeleteClip = { clipId -> clipboardManager.removeClip(clipId) },
                                onClearAll = { clipboardManager.clearAllUnpinned() },
                                onBackToKeyboard = { switchToKeyboardMode() }
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
                                onBackToKeyboard = { switchToKeyboardMode() },
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
                                        switchToKeyboardMode()
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
                                                switchToKeyboardMode()
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
                                    onCancel = { switchToKeyboardMode() },
                                    onSave = {
                                        serviceScope.launch {
                                            preferences.setKeyboardHeightRatio(previewHeightRatio)
                                            preferences.setBottomPaddingDp(previewBottomPaddingDp)
                                        }
                                        switchToKeyboardMode()
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

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        stopBackspaceRepeat()
        resolveInputType(attribute)
        resolveImeAction(attribute)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        installViewTreeOwners()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        stopBackspaceRepeat()
        currentComposingWord = ""
        currentSuggestions = emptyList()
        keyboardMode = KeyboardMode.KEYBOARD

        // Reset modifier state to guarantee clean QWERTY layer on each session (Issue 3 fix)
        modifierState = ModifierState()
        hasActiveSelection = false
        activeSelectionHandle = SelectionHandle.END
        selectionStartPos = 0
        selectionEndPos = 0
        isNumericOverriddenToAlpha = false

        resolveInputType(info)
        resolveImeAction(info)
        if (isEmailField || isUriField) {
            updateEmailOrUriSuggestions()
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        installViewTreeOwners()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        stopBackspaceRepeat()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        keyboardMode = KeyboardMode.KEYBOARD
        modifierState = ModifierState()
        hasActiveSelection = false
        activeSelectionHandle = SelectionHandle.END
        selectionStartPos = 0
        selectionEndPos = 0
        isNumericOverriddenToAlpha = false
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        stopBackspaceRepeat()
        currentComposingWord = ""
        currentSuggestions = emptyList()
        modifierState = ModifierState()
        hasActiveSelection = false
        activeSelectionHandle = SelectionHandle.END
        selectionStartPos = 0
        selectionEndPos = 0
        isNumericOverriddenToAlpha = false
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        selectionStartPos = minOf(newSelStart, newSelEnd)
        selectionEndPos = maxOf(newSelStart, newSelEnd)
        hasActiveSelection = (newSelStart != newSelEnd)

        // Reset composing word if cursor was moved manually
        if (newSelStart != oldSelStart || newSelEnd != oldSelEnd) {
            currentComposingWord = ""
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBackspaceRepeat()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        serviceScope.cancel()
        clipboardManager.destroy()
    }

    private fun resolveInputType(info: EditorInfo?) {
        val inputType = info?.inputType ?: 0
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val flags = inputType and InputType.TYPE_MASK_FLAGS
        val imeOptions = info?.imeOptions ?: 0
        val imeAction = imeOptions and EditorInfo.IME_MASK_ACTION
        val pkg = info?.packageName?.lowercase() ?: ""

        val isKnownTerminalPkg = pkg.contains("devbox") || pkg.contains("termux") ||
                pkg.contains("connectbot") || pkg.contains("juicessh") || pkg.contains("terminal")
        val hasRawTerminalFlags = (inputType == InputType.TYPE_NULL && isKnownTerminalPkg) ||
                (variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD &&
                 (flags and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0 &&
                 imeAction == EditorInfo.IME_ACTION_NONE &&
                 (isKnownTerminalPkg || (imeOptions and EditorInfo.IME_FLAG_NO_EXTRACT_UI) != 0))

        isTerminalField = hasRawTerminalFlags

        // Password detection: visible password is only a password if NOT a terminal
        isPasswordField = when (variation) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_NUMBER_VARIATION_PASSWORD -> true
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD -> !isTerminalField
            else -> false
        }

        // Email and URI field detection: disable word composing, disable autocorrect
        isEmailField = variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
        isUriField = variation == InputType.TYPE_TEXT_VARIATION_URI

        isNumberPadField = (inputClass == InputType.TYPE_CLASS_NUMBER) || (inputClass == InputType.TYPE_CLASS_DATETIME)
        isPhonePadField = (inputClass == InputType.TYPE_CLASS_PHONE)
        isDecimalNumberField = (flags and InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0
        isSignedNumberField = (flags and InputType.TYPE_NUMBER_FLAG_SIGNED) != 0
    }

    private fun resolveImeAction(info: EditorInfo?) {
        val imeOptions = info?.imeOptions ?: 0
        val inputType = info?.inputType ?: 0
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        val isMultiLine = (inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0
        val noEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

        if (noEnterAction || (isMultiLine && (action == EditorInfo.IME_ACTION_UNSPECIFIED || action == EditorInfo.IME_ACTION_NONE))) {
            currentActionId = EditorInfo.IME_ACTION_NONE
            currentActionIcon = R.drawable.ic_enter
            currentActionLabel = null
            return
        }

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

    private fun updateEmailOrUriSuggestions() {
        val ic = currentInputConnection ?: return
        if (isEmailField) {
            val textBefore = ic.getTextBeforeCursor(128, 0)?.toString() ?: ""
            val lastToken = textBefore.substringAfterLast(' ', textBefore).substringAfterLast('\n', textBefore)

            if (lastToken.contains('@')) {
                val prefixAfterAt = lastToken.substringAfterLast('@').lowercase()
                if (prefixAfterAt.isEmpty()) {
                    currentSuggestions = listOf("gmail.com", "yahoo.com", "outlook.com")
                } else {
                    val matches = commonEmailDomains.filter { it.startsWith(prefixAfterAt) }
                    currentSuggestions = if (matches.isNotEmpty()) matches.take(3) else emptyList()
                }
            } else if (lastToken.isEmpty()) {
                val defaultList = mutableListOf<String>()
                if (savedEmailsList.isNotEmpty()) {
                    defaultList.add(savedEmailsList.first())
                }
                defaultList.add("@gmail.com")
                defaultList.add("@yahoo.com")
                if (defaultList.size < 3) defaultList.add("@outlook.com")
                currentSuggestions = defaultList.take(3)
            } else {
                val defaultList = mutableListOf<String>()
                val matchedSaved = savedEmailsList.firstOrNull { it.startsWith(lastToken, ignoreCase = true) }
                if (matchedSaved != null) {
                    defaultList.add(matchedSaved)
                }
                defaultList.add("@gmail.com")
                defaultList.add("@yahoo.com")
                if (defaultList.size < 3) defaultList.add("@outlook.com")
                currentSuggestions = defaultList.take(3)
            }
            return
        }

        if (isUriField) {
            val textBefore = ic.getTextBeforeCursor(128, 0)?.toString() ?: ""
            val lastToken = textBefore.substringAfterLast(' ', textBefore).substringAfterLast('\n', textBefore)
            if (lastToken.contains('.')) {
                val suffixPrefix = "." + lastToken.substringAfterLast('.').lowercase()
                val matches = commonUriSuffixes.filter { it.startsWith(suffixPrefix) && it != suffixPrefix }
                currentSuggestions = if (matches.isNotEmpty()) matches.take(3) else listOf(".com", ".id", ".org")
            } else {
                currentSuggestions = listOf(".com", ".id", ".co.id")
            }
            return
        }
    }

    private fun commitTextDirectly(text: String) {
        triggerHaptic()
        val ic = currentInputConnection ?: return
        if (currentComposingWord.isNotEmpty() && !isPasswordField && !isEmailField && !isUriField) {
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
                    if (!modifierState.isCtrlLocked) {
                        modifierState = modifierState.copy(isCtrlActive = false)
                    }
                    if (modifierState.isAltActive && !modifierState.isAltLocked) {
                        modifierState = modifierState.copy(isAltActive = false)
                    }
                    if (modifierState.isShifted && !modifierState.isCapsLock) {
                        modifierState = modifierState.copy(isShifted = false)
                    }
                    return
                }

                val charToCommit = if (key.primaryLabel.length == 1 && key.primaryLabel[0].isLetter()) {
                    if (modifierState.isShiftActive) key.primaryLabel.uppercase() else key.primaryLabel.lowercase()
                } else {
                    key.primaryLabel
                }

                if (modifierState.isAltActive) {
                    if (isTerminalField) {
                        ic.commitText("\u001B$charToCommit", 1)
                    } else {
                        val keyCode = KeyEvent.keyCodeFromString("KEYCODE_${charToCommit.uppercase()}")
                        if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                            val meta = KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
                            val now = android.os.SystemClock.uptimeMillis()
                            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
                            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta))
                        } else {
                            ic.commitText("\u001B$charToCommit", 1)
                        }
                    }
                    if (!modifierState.isAltLocked) {
                        modifierState = modifierState.copy(isAltActive = false)
                    }
                    if (modifierState.isShifted && !modifierState.isCapsLock) {
                        modifierState = modifierState.copy(isShifted = false)
                    }
                    return
                }

                ic.commitText(charToCommit, 1)

                // Update composing word for passive suggestions (NEVER in terminal, password, email, or URI)
                if (enablePassiveSuggestions && !isTerminalField && !isPasswordField && !isEmailField && !isUriField && charToCommit.length == 1 && charToCommit[0].isLetter()) {
                    currentComposingWord += charToCommit
                    val textBefore = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
                    val lastToken = textBefore.substringAfterLast(' ', textBefore).substringAfterLast('\n', textBefore)
                    if (lastToken.contains('@')) {
                        val prefixAfterAt = lastToken.substringAfterLast('@').lowercase()
                        val matches = commonEmailDomains.filter { it.startsWith(prefixAfterAt) }
                        currentSuggestions = if (matches.isNotEmpty()) matches.take(3) else emptyList()
                    } else {
                        currentSuggestions = PassiveSuggestionEngine.getSuggestions(currentComposingWord)
                    }
                } else {
                    if (currentComposingWord.isNotEmpty() && !isTerminalField && !isPasswordField && !isEmailField && !isUriField) {
                        PassiveSuggestionEngine.learnWord(currentComposingWord)
                    }
                    currentComposingWord = ""
                    if (!isTerminalField && (isEmailField || isUriField)) {
                        updateEmailOrUriSuggestions()
                    } else if (!isTerminalField && charToCommit == "@" && !isPasswordField) {
                        currentSuggestions = listOf("gmail.com", "yahoo.com", "outlook.com")
                    } else {
                        currentSuggestions = emptyList()
                    }
                }

                // Auto reset shift if not caps lock
                if (modifierState.isShifted && !modifierState.isCapsLock) {
                    modifierState = modifierState.copy(isShifted = false)
                }
            }
            KeyType.SPACE -> {
                // 1. Terminal mode: completely raw pass-through, NEVER word-expand
                if (isTerminalField) {
                    currentComposingWord = ""
                    currentSuggestions = emptyList()
                    if (modifierState.isCtrlActive) {
                        ic.commitText("\u0000", 1)
                        if (!modifierState.isCtrlLocked) modifierState = modifierState.copy(isCtrlActive = false)
                        if (!modifierState.isAltLocked) modifierState = modifierState.copy(isAltActive = false)
                        return
                    }
                    if (modifierState.isAltActive) {
                        ic.commitText("\u001B ", 1)
                        if (!modifierState.isAltLocked) modifierState = modifierState.copy(isAltActive = false)
                        return
                    }
                    ic.commitText(" ", 1)
                    return
                }

                // 2. Read actual word boundary directly before cursor (GUARANTEES only 1 word is touched)
                val textBefore = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
                var wordEnd = textBefore.length - 1
                while (wordEnd >= 0 && textBefore[wordEnd].isWhitespace()) {
                    wordEnd--
                }
                var wordStart = wordEnd
                while (wordStart >= 0 && (textBefore[wordStart].isLetter() || textBefore[wordStart] == '\'')) {
                    wordStart--
                }
                val lastWord = if (wordEnd >= 0 && wordStart < wordEnd) {
                    textBefore.substring(wordStart + 1, wordEnd + 1)
                } else {
                    currentComposingWord
                }

                // 3. Custom text expansion shortcut (e.g. "akunpro" -> template expansion)
                if (lastWord.isNotEmpty()) {
                    val expansion = PassiveSuggestionEngine.getShortcutExpansion(lastWord)
                    if (expansion != null) {
                        ic.deleteSurroundingText(lastWord.length, 0)
                        ic.commitText("$expansion ", 1)
                        currentComposingWord = ""
                        currentSuggestions = emptyList()
                        return
                    }
                }

                // 4. Normal word learning on space
                if (lastWord.isNotEmpty() && !isPasswordField && !isEmailField && !isUriField) {
                    PassiveSuggestionEngine.learnWord(lastWord)
                }
                currentComposingWord = ""
                currentSuggestions = emptyList()

                ic.commitText(" ", 1)
            }
            KeyType.BACKSPACE -> {
                performBackspace()
            }
            KeyType.SHIFT -> {
                toggleShift()
            }
            KeyType.CTRL -> {
                toggleCtrl()
            }
            KeyType.FN -> {
                val nextFn = !modifierState.isFnActive
                modifierState = modifierState.copy(
                    isFnActive = nextFn,
                    isAltActive = if (!nextFn) false else modifierState.isAltActive
                )
            }
            KeyType.SYM -> {
                if (isNumberPadField || isPhonePadField) {
                    isNumericOverriddenToAlpha = !isNumericOverriddenToAlpha
                } else {
                    val nextSymActive = !modifierState.isSymActive
                    modifierState = modifierState.copy(
                        isSymActive = nextSymActive,
                        isSymPage2Active = false
                    )
                }
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
                if (currentComposingWord.isNotEmpty() && !isPasswordField && !isEmailField && !isUriField) {
                    PassiveSuggestionEngine.learnWord(currentComposingWord)
                }
                currentComposingWord = ""
                currentSuggestions = emptyList()

                if (isEmailField) {
                    val textBefore = ic.getTextBeforeCursor(128, 0)?.toString() ?: ""
                    val lastToken = textBefore.substringAfterLast(' ', textBefore).substringAfterLast('\n', textBefore)
                    if (lastToken.contains('@') && lastToken.contains('.') && lastToken.length >= 6) {
                        serviceScope.launch {
                            preferences.addSavedEmail(lastToken)
                        }
                    }
                }

                if (isTerminalField) {
                    when {
                        modifierState.isCtrlActive -> {
                            ic.commitText("\u001B[13;5u", 1)
                            modifierState = modifierState.copy(isCtrlActive = false, isAltActive = false)
                        }
                        modifierState.isAltActive -> {
                            ic.commitText("\u001B\r", 1)
                            modifierState = modifierState.copy(isAltActive = false)
                        }
                        modifierState.isShifted -> {
                            ic.commitText("\u001B[13;2u", 1)
                            if (!modifierState.isCapsLock) {
                                modifierState = modifierState.copy(isShifted = false)
                            }
                        }
                        else -> {
                            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                        }
                    }
                    return
                }

                if (currentActionId != EditorInfo.IME_ACTION_UNSPECIFIED && currentActionId != EditorInfo.IME_ACTION_NONE) {
                    val performed = ic.performEditorAction(currentActionId)
                    if (!performed) {
                        sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                    }
                } else {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
            }
            else -> {}
        }
    }

    private fun handleKeyLongClick(key: KeyModel) {
        triggerHaptic()
        val symbol = key.secondaryLabel ?: return
        val ic = currentInputConnection ?: return
        if (modifierState.isCtrlActive) {
            handleCtrlShortcut(symbol)
            if (!modifierState.isCtrlLocked) modifierState = modifierState.copy(isCtrlActive = false)
            if (!modifierState.isAltLocked) modifierState = modifierState.copy(isAltActive = false)
            return
        }
        if (modifierState.isAltActive && isTerminalField) {
            ic.commitText("\u001B$symbol", 1)
            if (!modifierState.isAltLocked) modifierState = modifierState.copy(isAltActive = false)
            return
        }
        ic.commitText(symbol, 1)
        currentComposingWord = ""
        currentSuggestions = emptyList()
        if (modifierState.isShifted && !modifierState.isCapsLock) {
            modifierState = modifierState.copy(isShifted = false)
        }
    }

    private fun handleCtrlShortcut(letter: String) {
        val ic = currentInputConnection ?: return
        val lower = letter.lowercase()

        if (isTerminalField) {
            val ch = lower.firstOrNull() ?: return
            val ctrlChar: Char? = when (ch) {
                in 'a'..'z' -> (ch - 'a' + 1).toChar()
                '[', '3' -> '\u001B' // ESC
                '\\', '4' -> '\u001C' // SIGQUIT
                ']', '5' -> '\u001D' // GS
                '^', '6' -> '\u001E' // RS
                '_', '-', '/', '7' -> '\u001F' // US (Undo)
                '@', '2', ' ' -> '\u0000' // NUL
                '?', '8' -> '\u007F' // DEL
                else -> null
            }
            if (ctrlChar != null) {
                val prefix = if (modifierState.isAltActive) "\u001B" else ""
                ic.commitText("$prefix$ctrlChar", 1)
                return
            }
        }

        // For clipboard/edit ops, use dual-dispatch: performContextMenuAction for
        // native EditText fields, PLUS sendKeyEvent for Chrome/WebView/apps that
        // only respond to KeyEvent with META_CTRL_ON. Both are safe to call.
        when (lower) {
            "a" -> {
                ic.performContextMenuAction(android.R.id.selectAll)
                sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_A)
            }
            "c" -> {
                ic.performContextMenuAction(android.R.id.copy)
                sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_C)
            }
            "v" -> {
                ic.performContextMenuAction(android.R.id.paste)
                sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_V)
            }
            "x" -> {
                ic.performContextMenuAction(android.R.id.cut)
                sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_X)
            }
            "z" -> {
                if (modifierState.isShiftActive) {
                    ic.performContextMenuAction(android.R.id.redo)
                    sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_Z)
                } else {
                    ic.performContextMenuAction(android.R.id.undo)
                    sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_Z)
                }
            }
            "y" -> {
                ic.performContextMenuAction(android.R.id.redo)
                sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_Y)
            }
            else -> {
                val keyCode = KeyEvent.keyCodeFromString("KEYCODE_${letter.uppercase()}")
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    sendCtrlKeyEvent(ic, keyCode)
                }
            }
        }
    }

    private fun sendCtrlKeyEvent(ic: android.view.inputmethod.InputConnection, keyCode: Int) {
        var meta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (modifierState.isAltActive) {
            meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        }
        if (modifierState.isShiftActive) {
            meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        }
        val now = android.os.SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta))
    }

    private fun performBackspace() {
        triggerHaptic()
        val ic = currentInputConnection ?: return
        if (isTerminalField) {
            if (modifierState.isAltActive) {
                ic.commitText("\u001B\u007F", 1)
                if (!modifierState.isAltLocked) modifierState = modifierState.copy(isAltActive = false)
                return
            }
            if (modifierState.isCtrlActive) {
                ic.commitText("\u0017", 1) // Ctrl+W (kill word backward)
                if (!modifierState.isCtrlLocked) modifierState = modifierState.copy(isCtrlActive = false)
                return
            }
        }
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            ic.commitText("", 1)
            hasActiveSelection = false
            return
        }

        // Ctrl + Backspace in normal apps: Delete word backward
        if (modifierState.isCtrlActive) {
            val textBefore = ic.getTextBeforeCursor(128, 0)?.toString() ?: ""
            if (textBefore.isNotEmpty()) {
                var i = textBefore.length - 1
                while (i >= 0 && textBefore[i].isWhitespace()) {
                    i--
                }
                while (i >= 0 && !textBefore[i].isWhitespace()) {
                    i--
                }
                val deleteCount = textBefore.length - (i + 1)
                if (deleteCount > 0) {
                    ic.deleteSurroundingText(deleteCount, 0)
                } else {
                    sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_DEL)
                }
            } else {
                sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_DEL)
            }
            if (!modifierState.isCtrlLocked) {
                modifierState = modifierState.copy(isCtrlActive = false)
            }
            currentComposingWord = ""
            currentSuggestions = emptyList()
            return
        }

        val deleted = ic.deleteSurroundingText(1, 0)
        if (!deleted) {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
        }
        if (currentComposingWord.isNotEmpty()) {
            currentComposingWord = currentComposingWord.dropLast(1)
            val textBefore = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
            val lastToken = textBefore.substringAfterLast(' ', textBefore).substringAfterLast('\n', textBefore)
            if (lastToken.contains('@')) {
                val prefixAfterAt = lastToken.substringAfterLast('@').lowercase()
                val matches = commonEmailDomains.filter { it.startsWith(prefixAfterAt) }
                currentSuggestions = if (matches.isNotEmpty()) matches.take(3) else emptyList()
            } else {
                currentSuggestions = PassiveSuggestionEngine.getSuggestions(currentComposingWord)
            }
        } else if (isEmailField || isUriField) {
            updateEmailOrUriSuggestions()
        } else if (!isTerminalField && !isPasswordField) {
            val textBefore = ic.getTextBeforeCursor(64, 0)?.toString() ?: ""
            val lastToken = textBefore.substringAfterLast(' ', textBefore).substringAfterLast('\n', textBefore)
            if (lastToken.endsWith('@')) {
                currentSuggestions = listOf("gmail.com", "yahoo.com", "outlook.com")
            } else {
                currentSuggestions = emptyList()
            }
        }
    }

    private fun moveCursor(offset: Int) {
        currentComposingWord = ""
        triggerHaptic()
        val ic = currentInputConnection ?: return
        val count = kotlin.math.abs(offset)
        val step = if (offset > 0) 1 else -1

        val isSelectionActive = hasActiveSelection || modifierState.isShiftActive

        if (isSelectionActive) {
            var currentStart = selectionStartPos
            var currentEnd = selectionEndPos

            for (i in 0 until count) {
                if (currentStart == currentEnd) {
                    if (activeSelectionHandle == SelectionHandle.START) {
                        currentStart = (currentStart + step).coerceAtLeast(0)
                    } else {
                        currentEnd = (currentEnd + step).coerceAtLeast(0)
                    }
                } else {
                    if (activeSelectionHandle == SelectionHandle.START) {
                        val nextStart = (currentStart + step).coerceAtLeast(0)
                        if (nextStart <= currentEnd) {
                            currentStart = nextStart
                        } else {
                            currentStart = currentEnd
                            currentEnd = nextStart
                            activeSelectionHandle = SelectionHandle.END
                        }
                    } else {
                        val nextEnd = (currentEnd + step).coerceAtLeast(0)
                        if (nextEnd >= currentStart) {
                            currentEnd = nextEnd
                        } else {
                            currentEnd = currentStart
                            currentStart = nextEnd
                            activeSelectionHandle = SelectionHandle.START
                        }
                    }
                }
            }

            selectionStartPos = currentStart
            selectionEndPos = currentEnd
            hasActiveSelection = (currentStart != currentEnd)
            ic.setSelection(currentStart, currentEnd)
            return
        }

        // Standard non-selection cursor movement
        val keyCode = if (offset > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        val isCtrl = modifierState.isCtrlActive

        if (isCtrl) {
            val meta = KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
            val now = android.os.SystemClock.uptimeMillis()
            for (i in 0 until count) {
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta))
            }
        } else {
            for (i in 0 until count) {
                sendDownUpKeyEvents(keyCode)
            }
        }
    }

    private fun commitSuggestion(word: String) {
        triggerHaptic()
        val ic = currentInputConnection ?: return

        if (isEmailField || word.contains("@") || word.endsWith(".com") || word.endsWith(".id") || word.endsWith(".org") || word.endsWith(".net") || word.endsWith(".io")) {
            val textBefore = ic.getTextBeforeCursor(128, 0)?.toString() ?: ""
            val lastToken = textBefore.substringAfterLast(' ', textBefore).substringAfterLast('\n', textBefore)

            val isFullEmail = !word.startsWith("@") && word.contains('@') && word.contains('.')
            val isDomainWithAt = word.startsWith("@")
            val isDomainWithoutAt = !isFullEmail && !isDomainWithAt && !word.startsWith(".") &&
                (commonEmailDomains.any { word.equals(it, ignoreCase = true) } ||
                 word.endsWith(".com") || word.endsWith(".id") || word.endsWith(".org") || word.endsWith(".net") || word.endsWith(".io"))
            val isUriSuffix = word.startsWith(".")

            if (isFullEmail) {
                // User picked a full email, e.g. "sahal@gmail.com" from saved emails
                if (lastToken.isNotEmpty()) {
                    ic.deleteSurroundingText(lastToken.length, 0)
                }
                ic.commitText(word, 1)
            } else if (isDomainWithAt) {
                // User picked e.g. "@gmail.com"
                if (lastToken.contains('@')) {
                    val prefixAfterAt = lastToken.substringAfterLast('@')
                    val charsToDelete = prefixAfterAt.length + 1
                    ic.deleteSurroundingText(charsToDelete, 0)
                    ic.commitText(word, 1)
                } else {
                    // User typed username (e.g. "budi"), NEVER delete username! Just append "@gmail.com"
                    ic.commitText(word, 1)
                }
            } else if (isDomainWithoutAt) {
                // User picked e.g. "gmail.com"
                if (lastToken.contains('@')) {
                    val prefixAfterAt = lastToken.substringAfterLast('@')
                    if (prefixAfterAt.isNotEmpty() && word.startsWith(prefixAfterAt, ignoreCase = true)) {
                        ic.deleteSurroundingText(prefixAfterAt.length, 0)
                    }
                    ic.commitText(word, 1)
                } else {
                    // User typed username without '@', prepend '@' so it completes to "budi@gmail.com"
                    ic.commitText("@$word", 1)
                }
            } else if (isUriSuffix) {
                // URI suffix e.g. ".com"
                if (lastToken.contains('.')) {
                    val suffix = "." + lastToken.substringAfterLast('.')
                    if (word.startsWith(suffix, ignoreCase = true)) {
                        ic.deleteSurroundingText(suffix.length, 0)
                    }
                }
                ic.commitText(word, 1)
            } else {
                ic.commitText(word, 1)
            }

            currentComposingWord = ""
            if (isEmailField || isUriField) {
                updateEmailOrUriSuggestions()
            } else {
                currentSuggestions = emptyList()
            }
            return
        }

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

        // Navigation arrows in selection mode move the active boundary
        if (hasActiveSelection || modifierState.isShiftActive) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                moveCursor(-1)
                return
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                moveCursor(1)
                return
            }
        }

        val shift = modifierState.isShiftActive
        val alt = modifierState.isAltActive
        val ctrl = modifierState.isCtrlActive
        val modMask = (if (shift) 1 else 0) + (if (alt) 2 else 0) + (if (ctrl) 4 else 0)
        val modCode = 1 + modMask

        if (isTerminalField) {
            // In terminal mode, emit standard VT100 / xterm escape sequences when needed
            val terminalSeq: String? = when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> if (modMask > 0) "\u001B[1;${modCode}A" else null
                KeyEvent.KEYCODE_DPAD_DOWN -> if (modMask > 0) "\u001B[1;${modCode}B" else null
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (modMask > 0) "\u001B[1;${modCode}C" else null
                KeyEvent.KEYCODE_DPAD_LEFT -> if (modMask > 0) "\u001B[1;${modCode}D" else null
                KeyEvent.KEYCODE_TAB -> when {
                    shift -> "\u001B[Z"
                    alt -> "\u001B\t"
                    else -> null
                }
                KeyEvent.KEYCODE_MOVE_HOME -> if (modMask > 0) "\u001B[1;${modCode}H" else "\u001B[H"
                KeyEvent.KEYCODE_MOVE_END -> if (modMask > 0) "\u001B[1;${modCode}F" else "\u001B[F"
                KeyEvent.KEYCODE_PAGE_UP -> if (modMask > 0) "\u001B[5;${modCode}~" else "\u001B[5~"
                KeyEvent.KEYCODE_PAGE_DOWN -> if (modMask > 0) "\u001B[6;${modCode}~" else "\u001B[6~"
                KeyEvent.KEYCODE_INSERT -> if (modMask > 0) "\u001B[2;${modCode}~" else "\u001B[2~"
                KeyEvent.KEYCODE_FORWARD_DEL -> if (modMask > 0) "\u001B[3;${modCode}~" else "\u001B[3~"
                KeyEvent.KEYCODE_F1 -> if (modMask > 0) "\u001B[1;${modCode}P" else "\u001BOP"
                KeyEvent.KEYCODE_F2 -> if (modMask > 0) "\u001B[1;${modCode}Q" else "\u001BOQ"
                KeyEvent.KEYCODE_F3 -> if (modMask > 0) "\u001B[1;${modCode}R" else "\u001BOR"
                KeyEvent.KEYCODE_F4 -> if (modMask > 0) "\u001B[1;${modCode}S" else "\u001BOS"
                KeyEvent.KEYCODE_F5 -> if (modMask > 0) "\u001B[15;${modCode}~" else "\u001B[15~"
                KeyEvent.KEYCODE_F6 -> if (modMask > 0) "\u001B[17;${modCode}~" else "\u001B[17~"
                KeyEvent.KEYCODE_F7 -> if (modMask > 0) "\u001B[18;${modCode}~" else "\u001B[18~"
                KeyEvent.KEYCODE_F8 -> if (modMask > 0) "\u001B[19;${modCode}~" else "\u001B[19~"
                KeyEvent.KEYCODE_F9 -> if (modMask > 0) "\u001B[20;${modCode}~" else "\u001B[20~"
                KeyEvent.KEYCODE_F10 -> if (modMask > 0) "\u001B[21;${modCode}~" else "\u001B[21~"
                KeyEvent.KEYCODE_F11 -> if (modMask > 0) "\u001B[23;${modCode}~" else "\u001B[23~"
                KeyEvent.KEYCODE_F12 -> if (modMask > 0) "\u001B[24;${modCode}~" else "\u001B[24~"
                else -> null
            }

            if (modMask > 0) {
                modifierState = modifierState.copy(
                    isCtrlActive = if (modifierState.isCtrlLocked) modifierState.isCtrlActive else false,
                    isAltActive = if (modifierState.isAltLocked) modifierState.isAltActive else false,
                    isShifted = if (modifierState.isCapsLock) modifierState.isShifted else false
                )
            }

            if (terminalSeq != null) {
                ic.commitText(terminalSeq, 1)
                return
            }

            // Unmodified ESC, TAB, and DPAD arrows use sendDownUpKeyEvents so DevBox handles DECCKM cursor mode natively
            sendDownUpKeyEvents(keyCode)
            return
        }

        // Standard Android / WebView mode
        var meta = 0
        if (ctrl) meta = meta or KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON
        if (alt) meta = meta or KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON
        if (shift) meta = meta or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON

        if (meta != 0) {
            val now = android.os.SystemClock.uptimeMillis()
            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta))
            modifierState = modifierState.copy(
                isCtrlActive = if (modifierState.isCtrlLocked) modifierState.isCtrlActive else false,
                isAltActive = if (modifierState.isAltLocked) modifierState.isAltActive else false,
                isShifted = if (modifierState.isCapsLock) modifierState.isShifted else false
            )
        } else {
            sendDownUpKeyEvents(keyCode)
        }
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
