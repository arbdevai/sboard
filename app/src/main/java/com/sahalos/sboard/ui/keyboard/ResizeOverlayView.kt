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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val overlayBg = Color.Black.copy(alpha = 0.45f)
    val controlBarBg = Color(theme.surfaceColor).copy(alpha = 0.95f)
    val textColor = Color(theme.keyTextColor)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overlayBg)
            .border(2.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
    ) {
        // Control Bar at Top (Batal, Reset, Simpan)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(controlBarBg)
                .padding(horizontal = 10.dp, vertical = 6.dp),
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
                    color = textColor.copy(alpha = 0.8f),
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
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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

        // Top Drag Handle (Adjust Height)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 46.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        // Drag up -> increase height, Drag down -> decrease height
                        val deltaRatio = -dragAmount.y / 250f
                        val newRatio = (currentHeightRatio + deltaRatio).coerceIn(0.75f, 1.35f)
                        onHeightRatioChange(newRatio)
                    }
                }
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor)
                .padding(horizontal = 24.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▲ Tarik Atas / Bawah (Tinggi) ▼",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Bottom Drag Handle (Adjust Bottom Margin / Ergonomics)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        // Drag up -> increase bottom padding, Drag down -> decrease
                        val deltaDp = (-dragAmount.y / 3f).toInt()
                        val newPadding = (currentBottomPaddingDp + deltaDp).coerceIn(0, 48)
                        onBottomPaddingChange(newPadding)
                    }
                }
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor)
                .padding(horizontal = 24.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▲ Tarik Posisi Bawah (${currentBottomPaddingDp} dp) ▼",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
