package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahalos.sboard.R
import com.sahalos.sboard.model.SboardTheme

@Composable
fun SuggestionsBar(
    suggestions: List<String>,
    theme: SboardTheme,
    isCtrlActive: Boolean,
    isFnActive: Boolean,
    onCtrlClick: () -> Unit,
    onFnClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onClipboardClick: () -> Unit,
    onResizeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barBgColor = Color(theme.surfaceColor)
    val textColor = Color(theme.keyTextColor)
    val accentColor = Color(theme.accentColor)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(barBgColor)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Left side: Tools (Clipboard & Resize) with comfortable spacing
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Clipboard button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onClipboardClick() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clipboard),
                    contentDescription = "Papan Klip",
                    tint = textColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Resize button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onResizeClick() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_resize),
                    contentDescription = "Atur Ukuran",
                    tint = textColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 2. Center: Suggestions or Sboard indicator
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (suggestions.isEmpty()) {
                Text(
                    text = "Sboard",
                    color = textColor.copy(alpha = 0.25f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                suggestions.take(3).forEach { word ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSuggestionClick(word) }
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = word,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 3. Right side: Modifiers (CTRL, FN) & Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Ctrl Button (Flat icon style, consistent with tools)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onCtrlClick() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CTRL",
                    color = if (isCtrlActive) accentColor else textColor.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = if (isCtrlActive) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            // Fn Button (Flat icon style, consistent with tools)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onFnClick() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FN",
                    color = if (isFnActive) accentColor else textColor.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = if (isFnActive) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            // Settings button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSettingsClick() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_settings),
                    contentDescription = "Pengaturan",
                    tint = textColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
