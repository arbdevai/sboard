package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    isCtrlLocked: Boolean = false,
    isShiftActive: Boolean = false,
    isCapsLock: Boolean = false,
    isSelecting: Boolean = false,
    hasSelection: Boolean = false,
    isFnActive: Boolean = false,
    onCtrlClick: () -> Unit,
    onShiftClick: () -> Unit = {},
    onSelectToggle: () -> Unit = {},
    onFnClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onClipboardClick: () -> Unit,
    onResizeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCutClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onPasteClick: () -> Unit = {},
    onSelectAllClick: () -> Unit = {},
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
            .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Left side: Modifiers (CTRL, SHIFT, SEL, FN)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // CTRL Button (Shows lock indicator if locked)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (isCtrlLocked) {
                            Modifier
                                .background(accentColor.copy(alpha = 0.22f))
                                .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onCtrlClick() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CTRL",
                    color = if (isCtrlActive || isCtrlLocked) accentColor else textColor.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    fontWeight = if (isCtrlActive || isCtrlLocked) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            // SHIFT Button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (isCapsLock) {
                            Modifier
                                .background(accentColor.copy(alpha = 0.22f))
                                .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onShiftClick() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isCapsLock) "CAPS" else "SHIFT",
                    color = if (isShiftActive) accentColor else textColor.copy(alpha = 0.85f),
                    fontSize = 10.5.sp,
                    fontWeight = if (isShiftActive) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            // SEL Button (Text Selection Mode Toggle)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (isSelecting) {
                            Modifier
                                .background(accentColor.copy(alpha = 0.25f))
                                .border(1.dp, accentColor, RoundedCornerShape(6.dp))
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelectToggle() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SEL",
                    color = if (isSelecting) accentColor else textColor.copy(alpha = 0.85f),
                    fontSize = 10.5.sp,
                    fontWeight = if (isSelecting) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            // FN Button
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
        }

        // 2. Center: Contextual Selection Action HUD OR Word Suggestions
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (hasSelection || isSelecting) {
                // PC Selection HUD: CUT, COPY, PASTE, ALL
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val actions = listOf(
                        "CUT" to onCutClick,
                        "COPY" to onCopyClick,
                        "PASTE" to onPasteClick,
                        "ALL" to onSelectAllClick
                    )
                    actions.forEach { (label, onClickAction) ->
                        Box(
                            modifier = Modifier
                                .fillMaxHeight(0.85f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(theme.keyBackgroundColor))
                                .clickable { onClickAction() }
                                .padding(horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (label == "ALL") accentColor else textColor.copy(alpha = 0.92f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp
                            )
                        }
                    }
                }
            } else if (suggestions.isEmpty()) {
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
                            .padding(horizontal = 5.dp),
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

        // 3. Right side: Tools (Clipboard/Paste, Resize, Settings)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Clipboard button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onClipboardClick() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_clipboard),
                    contentDescription = "Papan Klip",
                    tint = textColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Resize button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onResizeClick() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_resize),
                    contentDescription = "Atur Ukuran",
                    tint = textColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp)
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
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
