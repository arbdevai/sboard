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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
    modifier: Modifier = Modifier
) {
    val bgColor = Color(theme.backgroundColor)
    val surfaceColor = Color(theme.surfaceColor)
    val accentColor = Color(theme.accentColor)
    val textColor = Color(theme.keyTextColor)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(if (recentEmojis.isNotEmpty()) EmojiCategory.RECENT else EmojiCategory.SMILEYS) }

    val displayedEmojis = remember(searchQuery, selectedCategory, recentEmojis) {
        if (searchQuery.isNotBlank()) {
            EmojiProvider.search(searchQuery)
        } else if (selectedCategory == EmojiCategory.RECENT) {
            recentEmojis
        } else {
            EmojiProvider.getEmojisForCategory(selectedCategory)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(totalKeyboardHeight)
            .background(bgColor)
    ) {
        // 1. Search Bar & Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(surfaceColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back to Keyboard Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onBackToKeyboard() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_left),
                    contentDescription = "Kembali ke Keyboard",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Search Input Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(bgColor.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Cari",
                        tint = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari emoji (misal: senyum, api, hati...)",
                                color = textColor.copy(alpha = 0.45f),
                                fontSize = 12.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(accentColor),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { /* Done */ }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Hapus",
                                tint = textColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quick Delete Button in Header
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onBackspaceClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_backspace),
                    contentDescription = "Hapus",
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Category Tab Bar (Only when not actively searching)
        if (searchQuery.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(surfaceColor.copy(alpha = 0.7f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiCategory.values().forEach { cat ->
                    val isSelected = cat == selectedCategory
                    val tabBg = if (isSelected) accentColor.copy(alpha = 0.25f) else Color.Transparent
                    val tabBorder = if (isSelected) accentColor else Color.Transparent

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tabBg)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = cat.icon,
                                fontSize = 14.sp
                            )
                            Text(
                                text = cat.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else textColor.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Emoji Grid Container
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
                        text = if (searchQuery.isNotBlank()) {
                            "Tidak ada emoji yang cocok dengan '$searchQuery'"
                        } else {
                            "Belum ada emoji yang baru saja digunakan.\nKetik emoji dari kategori di atas!"
                        },
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(displayedEmojis, key = { it }) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onEmojiClick(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 23.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. Bottom Control Bar (ABC Switch & Quick Backspace)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(surfaceColor)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Return to ABC Keyboard
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(theme.keyBackgroundColor))
                    .clickable { onBackToKeyboard() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ABC",
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = if (searchQuery.isNotBlank()) "Hasil Pencarian" else selectedCategory.label,
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )

            // Backspace Key
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 32.dp)
                    .clip(RoundedCornerShape(8.dp))
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
