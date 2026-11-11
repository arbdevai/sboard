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
    private var currentComposingWord by mutableStateOf("")
    private var currentSuggestions by mutableStateOf<List<String>>(emptyList())

    // Dynamic Action Key
    private var currentActionIcon by mutableStateOf(R.drawable.ic_enter)
    private var currentActionLabel by mutableStateOf<String?>(null)
    private var currentActionId by mutableStateOf(EditorInfo.IME_ACTION_UNSPECIFIED)

    private var enableVibration = true
    private var isPasswordField = false
    private var isTerminalField = false
    private var isNumberPadField by mutableStateOf(false)
    private var isPhonePadField by mutableStateOf(false)
    private var isDecimalNumberField by mutableStateOf(false)
    private var isSignedNumberField by mutableStateOf(false)
    private var isNumericOverriddenToAlpha by mutableStateOf(false)
    private var backspaceRepeatJob: Job? = null

    private fun startBackspaceRepeat() {
        stopBackspaceRepeat()
        backspaceRepeatJob = serviceScope.launch {
            delay(350L)
            while (isActive) {
                performBackspace()
                delay(45L)
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
                                suggestions = if (isPasswordField || isNumberPadField || isPhonePadField) emptyList() else currentSuggestions,
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
                                        triggerHaptic()
                                        modifierState = modifierState.copy(isCtrlActive = !modifierState.isCtrlActive)
                                    }
                                },
                                onAltToggle = {
                                    triggerHaptic()
                                    modifierState = modifierState.copy(isAltActive = !modifierState.isAltActive)
                                },
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
        modifierState = modifierState.copy(
            isShifted = false,
            isCapsLock = false,
            isCtrlActive = false,
            isAltActive = false,
            isFnActive = false,
            isSymActive = false,
            isSymPage2Active = false
        )
        isNumericOverriddenToAlpha = false

        resolveInputType(info)
        resolveImeAction(info)
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
        modifierState = modifierState.copy(
            isShifted = false,
            isCapsLock = false,
            isCtrlActive = false,
            isAltActive = false,
            isFnActive = false,
            isSymActive = false,
            isSymPage2Active = false
        )
        isNumericOverriddenToAlpha = false
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        stopBackspaceRepeat()
        currentComposingWord = ""
        currentSuggestions = emptyList()
        modifierState = modifierState.copy(
            isShifted = false,
            isCapsLock = false,
            isCtrlActive = false,
            isAltActive = false,
            isFnActive = false,
            isSymActive = false,
            isSymPage2Active = false
        )
        isNumericOverriddenToAlpha = false
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
        isPasswordField = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD

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

    private fun commitTextDirectly(text: String) {
        triggerHaptic()
        val ic = currentInputConnection ?: return
        if (currentComposingWord.isNotEmpty() && !isPasswordField) {
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
                    modifierState = modifierState.copy(
                        isCtrlActive = false,
                        isAltActive = false,
                        isShifted = if (modifierState.isCapsLock) modifierState.isShifted else false
                    )
                    return
                }

                val charToCommit = if (modifierState.isShifted || modifierState.isCapsLock) {
                    key.primaryLabel.uppercase()
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
                            ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, meta))
                            ic.sendKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0, meta))
                        } else {
                            ic.commitText("\u001B$charToCommit", 1)
                        }
                    }
                    modifierState = modifierState.copy(
                        isAltActive = false,
                        isShifted = if (modifierState.isCapsLock) modifierState.isShifted else false
                    )
                    return
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
                if (currentComposingWord.isNotEmpty() && !isPasswordField) {
                    PassiveSuggestionEngine.learnWord(currentComposingWord)
                }
                currentComposingWord = ""
                currentSuggestions = emptyList()

                if (isTerminalField && modifierState.isCtrlActive) {
                    ic.commitText("\u0000", 1)
                    modifierState = modifierState.copy(isCtrlActive = false, isAltActive = false)
                    return
                }
                if (isTerminalField && modifierState.isAltActive) {
                    ic.commitText("\u001B ", 1)
                    modifierState = modifierState.copy(isAltActive = false)
                    return
                }
                ic.commitText(" ", 1)
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
                if (currentComposingWord.isNotEmpty() && !isPasswordField) {
                    PassiveSuggestionEngine.learnWord(currentComposingWord)
                }
                currentComposingWord = ""
                currentSuggestions = emptyList()

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
            modifierState = modifierState.copy(isCtrlActive = false, isAltActive = false)
            return
        }
        if (modifierState.isAltActive && isTerminalField) {
            ic.commitText("\u001B$symbol", 1)
            modifierState = modifierState.copy(isAltActive = false)
            return
        }
        ic.commitText(symbol, 1)
        currentComposingWord = ""
        currentSuggestions = emptyList()
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

        when (lower) {
            "a" -> {
                if (!ic.performContextMenuAction(android.R.id.selectAll)) {
                    sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_A)
                }
            }
            "c" -> {
                if (!ic.performContextMenuAction(android.R.id.copy)) {
                    sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_C)
                }
            }
            "v" -> {
                if (!ic.performContextMenuAction(android.R.id.paste)) {
                    sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_V)
                }
            }
            "x" -> {
                if (!ic.performContextMenuAction(android.R.id.cut)) {
                    sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_X)
                }
            }
            "z" -> {
                if (!ic.performContextMenuAction(android.R.id.undo)) {
                    sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_Z)
                }
            }
            "y" -> {
                if (!ic.performContextMenuAction(android.R.id.redo)) {
                    sendCtrlKeyEvent(ic, KeyEvent.KEYCODE_Y)
                }
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
        if (modifierState.isShifted) {
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
                modifierState = modifierState.copy(isAltActive = false)
                return
            }
            if (modifierState.isCtrlActive) {
                ic.commitText("\u0017", 1) // Ctrl+W (kill word backward)
                modifierState = modifierState.copy(isCtrlActive = false)
                return
            }
        }
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

        val shift = modifierState.isShifted
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
                    isCtrlActive = false,
                    isAltActive = false,
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
                isCtrlActive = false,
                isAltActive = false,
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
