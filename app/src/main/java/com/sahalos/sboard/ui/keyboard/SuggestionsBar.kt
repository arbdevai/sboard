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
import com.sahalos.sboard.model.SelectionHandle

@Composable
fun SuggestionsBar(
    suggestions: List<String>,
    theme: SboardTheme,
    isCtrlActive: Boolean,
    isCtrlLocked: Boolean = false,
    isShiftActive: Boolean = false,
    isCapsLock: Boolean = false,
    isFnActive: Boolean = false,
    hasSelection: Boolean = false,
    activeSelectionHandle: SelectionHandle = SelectionHandle.END,
    onCtrlClick: () -> Unit,
    onShiftClick: () -> Unit = {},
    onFnClick: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onClipboardClick: () -> Unit,
    onResizeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSelectHandleChange: (SelectionHandle) -> Unit = {},
    onDeselectClick: () -> Unit = {},
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
        // 1. Left side: Modifiers (CTRL, SHIFT, FN)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
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
                    .padding(horizontal = 7.dp),
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
                    .padding(horizontal = 7.dp),
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

            // FN Button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onFnClick() }
                    .padding(horizontal = 7.dp),
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

        // 2. Center: Dual-Endpoint Boundary Switcher ([◀ Awal] | [Akhir ▶]) OR Word Suggestions
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (hasSelection) {
                // Option 1: Endpoint Boundary Switcher
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Button: [◀ Awal] (Ujung Kiri)
                    val isStartActive = activeSelectionHandle == SelectionHandle.START
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isStartActive) accentColor.copy(alpha = 0.25f) else Color(theme.keyBackgroundColor)
                            )
                            .then(
                                if (isStartActive) {
                                    Modifier.border(1.dp, accentColor, RoundedCornerShape(6.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelectHandleChange(SelectionHandle.START) }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "◀ Awal",
                            color = if (isStartActive) accentColor else textColor.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = if (isStartActive) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }

                    // Button: [Akhir ▶] (Ujung Kanan)
                    val isEndActive = activeSelectionHandle == SelectionHandle.END
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isEndActive) accentColor.copy(alpha = 0.25f) else Color(theme.keyBackgroundColor)
                            )
                            .then(
                                if (isEndActive) {
                                    Modifier.border(1.dp, accentColor, RoundedCornerShape(6.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .clickable { onSelectHandleChange(SelectionHandle.END) }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Akhir ▶",
                            color = if (isEndActive) accentColor else textColor.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = if (isEndActive) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }

                    // Button: [Lepas] (Deselect)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(theme.keyBackgroundColor))
                            .clickable { onDeselectClick() }
                            .padding(horizontal = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lepas",
                            color = textColor.copy(alpha = 0.70f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Normal
                        )
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
                    val isDomainOrShortcut = word.startsWith("@") || word.contains("@") || word.startsWith(".") || word.length > 12
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(0.85f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDomainOrShortcut) accentColor.copy(alpha = 0.18f)
                                else Color(theme.keyBackgroundColor)
                            )
                            .border(
                                1.dp,
                                if (isDomainOrShortcut) accentColor.copy(alpha = 0.55f)
                                else textColor.copy(alpha = 0.18f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSuggestionClick(word) }
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = word,
                            color = if (isDomainOrShortcut) accentColor else textColor,
                            fontSize = if (word.length > 10) 11.sp else 12.5.sp,
                            fontWeight = FontWeight.Medium,
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
