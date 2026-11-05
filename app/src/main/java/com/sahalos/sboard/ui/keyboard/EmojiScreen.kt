package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahalos.sboard.R
import com.sahalos.sboard.core.EmojiCategory
import com.sahalos.sboard.core.EmojiProvider
import com.sahalos.sboard.model.SboardTheme

@Composable
fun EmojiScreen(
    theme: SboardTheme,
    recentEmojis: List<String>,
    totalKeyboardHeight: Dp,
    onEmojiClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onBackToKeyboard: () -> Unit,
    onOpenEmojiSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bgColor = Color(theme.backgroundColor)
    val surfaceColor = Color(theme.surfaceColor)
    val accentColor = Color(theme.accentColor)
    val textColor = Color(theme.keyTextColor)

    var selectedCategory by remember {
        mutableStateOf(if (recentEmojis.isNotEmpty()) EmojiCategory.RECENT else EmojiCategory.SMILEYS)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(totalKeyboardHeight)
            .background(bgColor)
    ) {
        // 1. Top Minimal Bar: Back | Search Pill (Opens Native Gboard-style QWERTY Search) | Backspace
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(surfaceColor)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back to ABC Keyboard
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onBackToKeyboard() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_left),
                    contentDescription = "Kembali ke Keyboard",
                    tint = textColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Clean Search Bar Pill (Tap to launch Gboard-style Search Mode)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(bgColor)
                    .clickable { onOpenEmojiSearch() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Cari Emoji",
                        tint = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Telusuri emoji...",
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quick Backspace Key
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { onBackspaceClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_backspace),
                    contentDescription = "Hapus",
                    tint = textColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // 2. Middle Area: Spacious 8-Column Emoji Grid (Over 80% screen area)
        val displayedEmojis = remember(selectedCategory, recentEmojis) {
            if (selectedCategory == EmojiCategory.RECENT) {
                recentEmojis
            } else {
                EmojiProvider.getEmojisForCategory(context, selectedCategory)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            if (displayedEmojis.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada emoji yang baru digunakan.\nPilih kategori di bawah untuk mulai!",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(displayedEmojis, key = { it }) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onEmojiClick(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Bottom Category Bar (Standard Gboard: ABC + Category Icons + Backspace)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .background(surfaceColor)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Return to Text Keyboard (ABC Button)
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(theme.keyBackgroundColor))
                    .clickable { onBackToKeyboard() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ABC",
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Category Icons Row (Clean Single Emoji Icons)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiCategory.values().forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { selectedCategory = cat },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat.icon,
                            fontSize = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Bottom Backspace Button
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(theme.keyBackgroundColor))
                    .clickable { onBackspaceClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_backspace),
                    contentDescription = "Hapus",
                    tint = textColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
