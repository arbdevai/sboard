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
    isClipboardOpen: Boolean,
    onSuggestionClick: (String) -> Unit,
    onClipboardToggle: () -> Unit,
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
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Quick tools: Clipboard button
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isClipboardOpen) accentColor.copy(alpha = 0.25f) else Color.Transparent)
                .clickable { onClipboardToggle() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_clipboard),
                contentDescription = "Clipboard",
                tint = if (isClipboardOpen) accentColor else textColor.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Suggestions Area
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (suggestions.isEmpty()) {
                Text(
                    text = "Sboard",
                    color = textColor.copy(alpha = 0.35f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                suggestions.forEach { word ->
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSuggestionClick(word) }
                            .padding(horizontal = 10.dp),
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

        // Settings button
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .clickable { onSettingsClick() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = "Pengaturan",
                tint = textColor.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
