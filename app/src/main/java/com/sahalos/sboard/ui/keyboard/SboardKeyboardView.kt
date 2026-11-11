package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sahalos.sboard.core.KeyLayoutProvider
import com.sahalos.sboard.core.NativeCoreBridge
import com.sahalos.sboard.core.PassiveSuggestionEngine
import com.sahalos.sboard.model.KeyModel
import com.sahalos.sboard.model.KeyType
import com.sahalos.sboard.model.ModifierState
import com.sahalos.sboard.model.SboardTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SboardKeyboardView(
    modifierState: ModifierState,
    theme: SboardTheme,
    suggestions: List<String>,
    bottomPaddingDp: Int,
    keyboardHeightRatio: Float,
    enableLongPressSymbols: Boolean,
    enableKeyPopup: Boolean = true,
    enableNumberRow: Boolean = false,
    isNumberPad: Boolean = false,
    isPhonePad: Boolean = false,
    isDecimalNumber: Boolean = false,
    isSignedNumber: Boolean = false,
    isNumericOverriddenToAlpha: Boolean = false,
    composingWord: String = "",
    actionImeLabel: String?,
    actionIconRes: Int,
    onKeyClick: (KeyModel) -> Unit,
    onKeyLongClick: (KeyModel) -> Unit,
    onBackspaceRepeat: () -> Unit = {},
    onBackspaceRepeatStart: () -> Unit = onBackspaceRepeat,
    onBackspaceRepeatStop: () -> Unit = {},
    onCursorMove: (Int) -> Unit,
    onShowImePicker: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onEmojiClick: () -> Unit,
    onClipboardClick: () -> Unit,
    onResizeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCtrlToggle: () -> Unit,
    onAltToggle: () -> Unit = {},
    onFnToggle: () -> Unit,
    onFunctionKeyClick: (Int) -> Unit,
    showSuggestionsBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val glideThresholdPx = with(density) { 12.dp.toPx() }
    val rolloverController = remember { KeyRolloverController() }
    val currentComposingWord by rememberUpdatedState(composingWord)

    DisposableEffect(Unit) {
        onDispose {
            onBackspaceRepeatStop()
        }
    }

    val isNumpadActive = isNumberPad && !isNumericOverriddenToAlpha
    val isPhonepadActive = isPhonePad && !isNumericOverriddenToAlpha

    val rows = when {
        isNumpadActive -> KeyLayoutProvider.getNumberPadRows(isDecimal = isDecimalNumber, isSigned = isSignedNumber)
        isPhonepadActive -> KeyLayoutProvider.getPhonePadRows()
        modifierState.isSymPage2Active -> KeyLayoutProvider.getSymbolsPage2Rows()
        modifierState.isSymActive -> KeyLayoutProvider.getSymbolsRows()
        else -> {
            val qwerty = KeyLayoutProvider.getQwertyRows()
            val adjustedQwerty = if (isNumberPad || isPhonePad) {
                qwerty.map { row ->
                    row.map { k ->
                        if (k.type == KeyType.SYM) k.copy(primaryLabel = "123") else k
                    }
                }
            } else {
                qwerty
            }
            if (enableNumberRow) {
                listOf(KeyLayoutProvider.getNumberRow()) + adjustedQwerty
            } else {
                adjustedQwerty
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(theme.backgroundColor))
    ) {
        // Suggestions & Quick Action Strip (CTRL, FN on Left; Suggestions Center; Clipboard, Resize, Settings on Right)
        val shouldShowSuggestionsBar = showSuggestionsBar && !isNumpadActive && !isPhonepadActive
        if (shouldShowSuggestionsBar) {
            SuggestionsBar(
                suggestions = suggestions,
                theme = theme,
                isCtrlActive = modifierState.isCtrlActive,
                isFnActive = modifierState.isFnActive,
                onCtrlClick = onCtrlToggle,
                onFnClick = onFnToggle,
                onSuggestionClick = onSuggestionClick,
                onClipboardClick = onClipboardClick,
                onResizeClick = onResizeClick,
                onSettingsClick = onSettingsClick
            )
        }

        // Optional Fn Row when Fn mode is active
        if (modifierState.isFnActive) {
            FnRowView(
                theme = theme,
                isAltActive = modifierState.isAltActive,
                onAltToggle = onAltToggle,
                onFunctionKeyClick = onFunctionKeyClick
            )
        }

        // Main Keyboard Area with Fixed Total Content Height (Prevents window jumping between QWERTY & Symbols)
        val has5RowsConfigured = enableNumberRow && !isNumpadActive && !isPhonepadActive
        val totalContentHeight = if (has5RowsConfigured) (210 * keyboardHeightRatio) else (192 * keyboardHeightRatio)
        val baseRowHeight = (totalContentHeight / rows.size.coerceAtLeast(1)).dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 1.dp)
        ) {
            rows.forEachIndexed { rowIndex, rowKeys ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(baseRowHeight)
                ) {
                    rowKeys.forEachIndexed { keyIndex, key ->
                        val keyWeight = key.weight

                        when (key.type) {
                            KeyType.SPACER -> {
                                Spacer(
                                    modifier = Modifier
                                        .weight(keyWeight)
                                        .fillMaxHeight()
                                )
                            }
                            KeyType.SPACE -> {
                                val currentOnKeyClick by rememberUpdatedState(onKeyClick)
                                val currentOnCursorMove by rememberUpdatedState(onCursorMove)
                                val currentOnShowImePicker by rememberUpdatedState(onShowImePicker)

                                Box(
                                    modifier = Modifier
                                        .weight(keyWeight)
                                        .fillMaxHeight()
                                        .pointerInput(key) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                // Immediately flush any held character key so fast typing before Space never swaps order
                                                rolloverController.flushAll()

                                                val startX = down.position.x
                                                val startY = down.position.y
                                                var isDragging = false
                                                var hasTriggeredLongPress = false
                                                var dragAccumulatorPx = 0f

                                                val longPressJob = coroutineScope.launch {
                                                    delay(450L)
                                                    if (!isDragging) {
                                                        hasTriggeredLongPress = true
                                                        currentOnShowImePicker()
                                                    }
                                                }

                                                try {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                                        if (!change.pressed) {
                                                            // Finger lifted
                                                            if (!isDragging && !hasTriggeredLongPress) {
                                                                currentOnKeyClick(key)
                                                            }
                                                            break
                                                        }

                                                        val totalDx = change.position.x - startX
                                                        val totalDy = change.position.y - startY

                                                        if (!isDragging && !hasTriggeredLongPress) {
                                                            if (kotlin.math.abs(totalDx) > 16f && kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy) * 1.2f) {
                                                                isDragging = true
                                                                longPressJob.cancel()
                                                            }
                                                        }

                                                        if (isDragging) {
                                                            val deltaX = change.position.x - change.previousPosition.x
                                                            dragAccumulatorPx += deltaX
                                                            if (dragAccumulatorPx >= glideThresholdPx) {
                                                                currentOnCursorMove(1)
                                                                dragAccumulatorPx -= glideThresholdPx
                                                            } else if (dragAccumulatorPx <= -glideThresholdPx) {
                                                                currentOnCursorMove(-1)
                                                                dragAccumulatorPx += glideThresholdPx
                                                            }
                                                            change.consume()
                                                        }
                                                    }
                                                } finally {
                                                    longPressJob.cancel()
                                                }
                                            }
                                        }
                                ) {
                                    KeyView(
                                        key = key,
                                        modifierState = modifierState,
                                        theme = theme,
                                        modifier = Modifier.fillMaxSize(),
                                        onClick = null,
                                        onLongClick = null
                                    )
                                }
                            }
                            KeyType.BACKSPACE -> {
                                Box(
                                    modifier = Modifier
                                        .weight(keyWeight)
                                        .fillMaxHeight()
                                        .pointerInput(key) {
                                            awaitEachGesture {
                                                val down = awaitFirstDown(requireUnconsumed = false)
                                                // Flush any pending key before deleting
                                                rolloverController.flushAll()
                                                onKeyClick(key)
                                                onBackspaceRepeatStart()
                                                try {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val change = event.changes.firstOrNull { it.id == down.id }
                                                        if (change == null || !change.pressed) {
                                                            break
                                                        }
                                                    }
                                                } finally {
                                                    onBackspaceRepeatStop()
                                                }
                                            }
                                        }
                                ) {
                                    KeyView(
                                        key = key,
                                        modifierState = modifierState,
                                        theme = theme,
                                        modifier = Modifier.fillMaxSize(),
                                        onClick = null,
                                        onLongClick = null
                                    )
                                }
                            }
                            else -> {
                                KeyView(
                                    key = key,
                                    modifierState = modifierState,
                                    theme = theme,
                                    actionImeLabel = actionImeLabel,
                                    actionIconRes = actionIconRes,
                                    enableKeyPopup = enableKeyPopup,
                                    isTopRow = (rowIndex == 0),
                                    modifier = Modifier
                                        .weight(keyWeight)
                                        .fillMaxHeight(),
                                    rolloverController = rolloverController,
                                    onClickWithCoord = { normX, normY ->
                                        val resolvedKey = resolveSpatialAndBigramKey(
                                            tappedKey = key,
                                            keyIndex = keyIndex,
                                            rowKeys = rowKeys,
                                            normX = normX,
                                            normY = normY,
                                            composingWord = currentComposingWord
                                        )
                                        onKeyClick(resolvedKey)
                                    },
                                    onLongClick = if (enableLongPressSymbols && !key.secondaryLabel.isNullOrEmpty()) {
                                        { onKeyLongClick(key) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }

        // Adjustable Bottom Padding (Ergonomi jempol)
        if (bottomPaddingDp > 0) {
            Spacer(modifier = Modifier.height(bottomPaddingDp.dp))
        }
    }
}

private fun resolveSpatialAndBigramKey(
    tappedKey: KeyModel,
    keyIndex: Int,
    rowKeys: List<KeyModel>,
    normX: Float,
    normY: Float,
    composingWord: String
): KeyModel {
    if (tappedKey.type != KeyType.CHARACTER || tappedKey.primaryLabel.length != 1 || !tappedKey.primaryLabel[0].isLetter()) {
        return tappedKey
    }

    // Only intervene if touch landed in the outer 16% horizontal border zone between adjacent character keys
    val neighborKey = when {
        normX < 0.16f && keyIndex > 0 -> rowKeys[keyIndex - 1]
        normX > 0.84f && keyIndex < rowKeys.lastIndex -> rowKeys[keyIndex + 1]
        else -> null
    }

    if (neighborKey == null || neighborKey.type != KeyType.CHARACTER ||
        neighborKey.primaryLabel.length != 1 || !neighborKey.primaryLabel[0].isLetter()
    ) {
        return tappedKey
    }

    // 1. Dictionary prefix check when composing a word
    if (composingWord.isNotEmpty()) {
        val prefixTapped = composingWord + tappedKey.primaryLabel
        val prefixNeighbor = composingWord + neighborKey.primaryLabel
        val validTapped = PassiveSuggestionEngine.hasPrefixMatch(prefixTapped)
        val validNeighbor = PassiveSuggestionEngine.hasPrefixMatch(prefixNeighbor)

        if (!validTapped && validNeighbor) {
            return neighborKey
        }
        if (validTapped && !validNeighbor) {
            return tappedKey
        }
    }

    // 2. Native Rust Spatial Gaussian + Bigram Probability resolution
    val prevChar = composingWord.lastOrNull() ?: ' '
    val unitWidth = 100f
    val unitHeight = 100f
    // Tapped key center is at X = 50, neighbor center is at X = -50 (left) or X = +150 (right)
    val neighborCenterX = if (normX < 0.16f) -50f else 150f
    val flatKeyData = floatArrayOf(
        tappedKey.code.toFloat(), 50f, 50f, unitWidth, unitHeight,
        neighborKey.code.toFloat(), neighborCenterX, 50f, unitWidth, unitHeight
    )
    val touchX = normX * unitWidth
    val touchY = normY * unitHeight

    val resolvedCode = NativeCoreBridge.resolveHit(touchX, touchY, prevChar, flatKeyData)
    return if (resolvedCode == neighborKey.code) {
        neighborKey
    } else {
        tappedKey
    }
}
