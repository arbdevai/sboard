package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.sahalos.sboard.R
import com.sahalos.sboard.core.KeyLayoutProvider
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
    actionImeLabel: String?,
    actionIconRes: Int,
    onKeyClick: (KeyModel) -> Unit,
    onKeyLongClick: (KeyModel) -> Unit,
    onBackspaceRepeat: () -> Unit,
    onCursorMove: (Int) -> Unit,
    onShowImePicker: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onEmojiClick: () -> Unit,
    onClipboardClick: () -> Unit,
    onResizeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onFunctionKeyClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val glideThresholdPx = with(density) { 12.dp.toPx() }

    val rows = when {
        modifierState.isSymPage2Active -> KeyLayoutProvider.getSymbolsPage2Rows()
        modifierState.isSymActive -> KeyLayoutProvider.getSymbolsRows()
        else -> KeyLayoutProvider.getQwertyRows()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(theme.backgroundColor))
    ) {
        // Suggestions & Quick Action Strip (Emoji, Clipboard, Resize, Settings)
        SuggestionsBar(
            suggestions = suggestions,
            theme = theme,
            onSuggestionClick = onSuggestionClick,
            onEmojiClick = onEmojiClick,
            onClipboardClick = onClipboardClick,
            onResizeClick = onResizeClick,
            onSettingsClick = onSettingsClick
        )

        // Optional Fn Row when Fn mode is active
        if (modifierState.isFnActive) {
            FnRowView(
                theme = theme,
                onFunctionKeyClick = onFunctionKeyClick
            )
        }

        // Main Keyboard Area
        val baseRowHeight = (48 * keyboardHeightRatio).dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            rows.forEachIndexed { rowIndex, rowKeys ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(baseRowHeight)
                ) {
                    rowKeys.forEach { key ->
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
                                var repeatJob by remember { mutableStateOf<Job?>(null) }
                                Box(
                                    modifier = Modifier
                                        .weight(keyWeight)
                                        .fillMaxHeight()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    // Instant tap on ACTION_DOWN
                                                    onKeyClick(key)
                                                    // Controlled repeat delete: 350ms initial delay, 45ms repeat
                                                    repeatJob = coroutineScope.launch {
                                                        delay(350L)
                                                        while (true) {
                                                            onBackspaceRepeat()
                                                            delay(45L)
                                                        }
                                                    }
                                                    try {
                                                        awaitRelease()
                                                    } finally {
                                                        repeatJob?.cancel()
                                                        repeatJob = null
                                                    }
                                                }
                                            )
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
                                    onClick = { onKeyClick(key) },
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
