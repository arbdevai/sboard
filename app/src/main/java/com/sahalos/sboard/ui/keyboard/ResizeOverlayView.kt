package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahalos.sboard.R
import com.sahalos.sboard.model.SboardTheme
import kotlin.math.abs

@Composable
fun ResizeOverlayView(
    currentHeightRatio: Float,
    currentBottomPaddingDp: Int,
    theme: SboardTheme,
    onHeightRatioChange: (Float) -> Unit,
    onBottomPaddingChange: (Int) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = Color(theme.accentColor)
    val overlayBg = Color.Black.copy(alpha = 0.50f)
    val controlBarBg = Color(theme.surfaceColor).copy(alpha = 0.95f)
    val textColor = Color(theme.keyTextColor)

    val currentRatioState = rememberUpdatedState(currentHeightRatio)
    val onHeightRatioChangeState = rememberUpdatedState(onHeightRatioChange)
    var accumulatedRatio by remember { mutableFloatStateOf(currentHeightRatio) }

    val currentPaddingState = rememberUpdatedState(currentBottomPaddingDp)
    val onBottomPaddingChangeState = rememberUpdatedState(onBottomPaddingChange)
    var accumulatedPadding by remember { mutableFloatStateOf(currentBottomPaddingDp.toFloat()) }

    val presets = listOf(
        0.85f to "85%",
        1.00f to "100%",
        1.15f to "115%",
        1.25f to "125%"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overlayBg)
            .border(2.dp, accentColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
    ) {
        // Top Container: Action Bar + Quick Presets + Top Drag Handle
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Control Bar at Top (Batal, Reset, Simpan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(controlBarBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onCancel() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Batal",
                        tint = textColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Batal", color = textColor, fontSize = 12.sp)
                }

                // Info & Reset
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onReset() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tinggi ${(currentHeightRatio * 100).toInt()}% • Reset",
                        color = textColor.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Save Button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor)
                        .clickable { onSave() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = "Simpan",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simpan", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Quick Preset Pills (85%, 100%, 115%, 125%)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { (ratio, label) ->
                    val isSelected = abs(currentHeightRatio - ratio) < 0.04f
                    val pillBg = if (isSelected) accentColor else controlBarBg.copy(alpha = 0.85f)
                    val pillTextColor = if (isSelected) Color.White else textColor

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(pillBg)
                            .clickable {
                                onHeightRatioChange(ratio)
                            }
                            .padding(horizontal = 12.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = pillTextColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Top Drag Handle (Adjust Height Smoothly)
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                accumulatedRatio = currentRatioState.value
                            },
                            onDrag = { _, dragAmount ->
                                // Drag up -> increase height, Drag down -> decrease height
                                accumulatedRatio = (accumulatedRatio - dragAmount.y / 350f).coerceIn(0.80f, 1.25f)
                                onHeightRatioChangeState.value(accumulatedRatio)
                            }
                        )
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.9f))
                    .padding(horizontal = 18.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▲ Tarik Manual ▼",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Drag Handle (Adjust Bottom Margin / Ergonomics)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        accumulatedPadding = currentPaddingState.value.toFloat()
                    },
                    onDrag = { _, dragAmount ->
                        // Drag up -> increase bottom padding, Drag down -> decrease
                        accumulatedPadding = (accumulatedPadding - dragAmount.y / 4f).coerceIn(0f, 36f)
                        onBottomPaddingChangeState.value(accumulatedPadding.toInt())
                    }
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor)
            .padding(horizontal = 20.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▲ Jarak Bawah: ${currentBottomPaddingDp} dp ▼",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
