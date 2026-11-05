package com.sahalos.sboard.service

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
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
import com.sahalos.sboard.model.ModifierState
import com.sahalos.sboard.model.SboardTheme
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

    // Keyboard state
    private var modifierState by mutableStateOf(ModifierState())
    private var currentComposingWord by mutableStateOf("")
    private var currentSuggestions by mutableStateOf<List<String>>(emptyList())
    private var isClipboardOpen by mutableStateOf(false)

    // Dynamic Action Key
    private var currentActionIcon by mutableStateOf(R.drawable.ic_enter)
    private var currentActionLabel by mutableStateOf<String?>(null)
    private var currentActionId by mutableStateOf(EditorInfo.IME_ACTION_UNSPECIFIED)

    private var enableVibration = true
    private var isPasswordField = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        preferences = SboardPreferences(this)
        clipboardManager = SboardClipboardManager(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        serviceScope.launch {
            preferences.enableVibration.collect { enableVibration = it }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onCreateInputView(): View {
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@SboardIME)
            setViewTreeViewModelStoreOwner(this@SboardIME)
            setViewTreeSavedStateRegistryOwner(this@SboardIME)

            setContent {
                val bottomPadding by preferences.bottomPaddingDp.collectAsStateWithLifecycle(initialValue = 0)
                val heightRatio by preferences.keyboardHeightRatio.collectAsStateWithLifecycle(initialValue = 1.0f)
                val enableLongPressSymbols by preferences.enableLongPressSymbols.collectAsStateWithLifecycle(initialValue = true)
                val themeId by preferences.themeId.collectAsStateWithLifecycle(initialValue = "nord_dark")
                val clips by clipboardManager.clips.collectAsStateWithLifecycle()

                val activeTheme = SboardTheme.AvailableThemes.find { it.id == themeId } ?: SboardTheme.NordDark

                SboardKeyboardView(
                    modifierState = modifierState,
                    theme = activeTheme,
                    suggestions = if (isPasswordField) emptyList() else currentSuggestions,
                    clips = clips,
                    isClipboardOpen = isClipboardOpen,
                    bottomPaddingDp = bottomPadding,
                    keyboardHeightRatio = heightRatio,
                    enableLongPressSymbols = enableLongPressSymbols,
                    actionImeLabel = currentActionLabel,
                    actionIconRes = currentActionIcon,
                    onKeyClick = { key -> handleKeyClick(key) },
                    onKeyLongClick = { key -> handleKeyLongClick(key) },
                    onBackspaceRepeat = { performBackspace() },
                    onCursorMove = { offset -> moveCursor(offset) },
                    onShowImePicker = { showImePicker() },
                    onSuggestionClick = { word -> commitSuggestion(word) },
                    onClipboardToggle = { isClipboardOpen = !isClipboardOpen },
                    onTogglePin = { clipId -> clipboardManager.togglePin(clipId) },
                    onClearClips = { clipboardManager.clearAllUnpinned() },
                    onSettingsClick = { openSettings() },
                    onFunctionKeyClick = { keyCode -> sendFunctionKey(keyCode) }
                )
            }
        }
        return composeView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        currentComposingWord = ""
        currentSuggestions = emptyList()
        isClipboardOpen = false

        // Check if password field
        val variation = (info?.inputType ?: 0) and InputType.TYPE_MASK_VARIATION
        isPasswordField = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

        // Evaluate Dynamic Action Key
        resolveImeAction(info)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        currentComposingWord = ""
        currentSuggestions = emptyList()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        isClipboardOpen = false
    }

    override fun onDestroy() {
        super.onDestroy()
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

    private fun handleKeyClick(key: KeyModel) {
        triggerHaptic()

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

                currentInputConnection?.commitText(charToCommit, 1)

                // Update composing word for passive suggestions
                if (!isPasswordField && charToCommit.length == 1 && charToCommit[0].isLetter()) {
                    currentComposingWord += charToCommit
                    currentSuggestions = PassiveSuggestionEngine.getSuggestions(currentComposingWord)
                } else {
                    currentComposingWord = ""
                    currentSuggestions = emptyList()
                }

                // Auto reset shift if not caps lock
                if (modifierState.isShifted && !modifierState.isCapsLock) {
                    modifierState = modifierState.copy(isShifted = false)
                }
            }
            KeyType.SPACE -> {
                currentInputConnection?.commitText(" ", 1)
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
                modifierState = modifierState.copy(isSymActive = !modifierState.isSymActive)
            }
            KeyType.ACTION -> {
                if (currentActionId != EditorInfo.IME_ACTION_UNSPECIFIED && currentActionId != EditorInfo.IME_ACTION_NONE) {
                    currentInputConnection?.performEditorAction(currentActionId)
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
        currentInputConnection?.commitText(symbol, 1)
        currentComposingWord = ""
        currentSuggestions = emptyList()
    }

    private fun handleCtrlShortcut(letter: String) {
        val lower = letter.lowercase()
        when (lower) {
            "a" -> currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
            "c" -> currentInputConnection?.performContextMenuAction(android.R.id.copy)
            "v" -> currentInputConnection?.performContextMenuAction(android.R.id.paste)
            "x" -> currentInputConnection?.performContextMenuAction(android.R.id.cut)
            "z" -> currentInputConnection?.performContextMenuAction(android.R.id.undo)
            else -> {
                // Send standard KeyEvent with META_CTRL_ON
                val keyCode = KeyEvent.keyCodeFromString("KEYCODE_${letter.uppercase()}")
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    val eventDown = KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_CTRL_ON)
                    val eventUp = KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0, KeyEvent.META_CTRL_ON)
                    currentInputConnection?.sendKeyEvent(eventDown)
                    currentInputConnection?.sendKeyEvent(eventUp)
                }
            }
        }
    }

    private fun performBackspace() {
        triggerHaptic()
        val selectedText = currentInputConnection?.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
            currentInputConnection?.commitText("", 1)
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        if (currentComposingWord.isNotEmpty()) {
            currentComposingWord = currentComposingWord.dropLast(1)
            currentSuggestions = PassiveSuggestionEngine.getSuggestions(currentComposingWord)
        }
    }

    private fun moveCursor(offset: Int) {
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(1000, 0)?.length ?: 0
        val targetPos = (before + offset).coerceAtLeast(0)
        ic.setSelection(targetPos, targetPos)
    }

    private fun commitSuggestion(word: String) {
        triggerHaptic()
        val ic = currentInputConnection ?: return
        if (currentComposingWord.isNotEmpty()) {
            ic.deleteSurroundingText(currentComposingWord.length, 0)
        }
        ic.commitText("$word ", 1)
        currentComposingWord = ""
        currentSuggestions = emptyList()
    }

    private fun sendFunctionKey(keyCode: Int) {
        triggerHaptic()
        val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        currentInputConnection?.sendKeyEvent(eventDown)
        currentInputConnection?.sendKeyEvent(eventUp)
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
