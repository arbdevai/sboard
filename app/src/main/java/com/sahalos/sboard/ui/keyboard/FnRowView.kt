package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahalos.sboard.core.KeyLayoutProvider
import com.sahalos.sboard.model.SboardTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FnRowView(
    theme: SboardTheme,
    isAltActive: Boolean,
    onAltToggle: () -> Unit,
    onFunctionKeyClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val navKeys = KeyLayoutProvider.getTerminalNavRow()
    val fnKeys = KeyLayoutProvider.getFunctionKeysRow()
    val barBgColor = Color(theme.surfaceColor)
    val keyBgColor = Color(theme.keyBackgroundColor)
    val textColor = Color(theme.keyTextColor)
    val accentColor = Color(theme.accentColor)
    val coroutineScope = rememberCoroutineScope()
    val currentOnFnKeyClick by rememberUpdatedState(onFunctionKeyClick)
    val currentOnAltToggle by rememberUpdatedState(onAltToggle)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBgColor)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Row 1: Terminal & Navigation Deck (ESC, TAB, ALT, ←, ↓, ↑, →, HOME, END, PGUP, PGDN, DEL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navKeys.forEach { key ->
                val isRepeatable = key.code in listOf(19, 20, 21, 22, 112)
                val isAltKey = key.code == -30
                val isArrow = key.code in listOf(19, 20, 21, 22)
                val bg = if (isAltKey && isAltActive) accentColor.copy(alpha = 0.22f) else keyBgColor
                val labelColor = when {
                    isAltKey && isAltActive -> accentColor
                    isArrow -> accentColor
                    else -> textColor.copy(alpha = 0.92f)
                }

                val interactionMod = if (isRepeatable) {
                    Modifier.pointerInput(key.code) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            currentOnFnKeyClick(key.code)
                            var repeatJob: Job? = coroutineScope.launch {
                                delay(360L)
                                while (true) {
                                    currentOnFnKeyClick(key.code)
                                    delay(55L)
                                }
                            }
                            try {
                                waitForUpOrCancellation()
                            } finally {
                                repeatJob?.cancel()
                                repeatJob = null
                            }
                        }
                    }
                } else {
                    Modifier.clickable {
                        if (isAltKey) {
                            currentOnAltToggle()
                        } else {
                            currentOnFnKeyClick(key.code)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(key.weight)
                        .fillMaxHeight()
                        .shadow(1.dp, shape = RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(bg)
                        .then(
                            if (isAltKey && isAltActive) {
                                Modifier.border(1.dp, accentColor, RoundedCornerShape(4.dp))
                            } else {
                                Modifier
                            }
                        )
                        .then(interactionMod),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key.primaryLabel,
                        color = labelColor,
                        fontSize = if (isArrow) 13.sp else 9.5.sp,
                        fontWeight = if (isArrow || (isAltKey && isAltActive)) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        // Row 2: Function Keys Strip (F1 - F12)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(31.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fnKeys.forEach { key ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(1.dp, shape = RoundedCornerShape(4.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(keyBgColor)
                        .clickable { currentOnFnKeyClick(key.code) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = key.primaryLabel,
                        color = accentColor,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
