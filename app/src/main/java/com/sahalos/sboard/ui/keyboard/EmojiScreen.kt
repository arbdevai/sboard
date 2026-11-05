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
    modifier: Modifier = Modifier
) {
    val bgColor = Color(theme.backgroundColor)
    val surfaceColor = Color(theme.surfaceColor)
    val accentColor = Color(theme.accentColor)
    val textColor = Color(theme.keyTextColor)

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(if (recentEmojis.isNotEmpty()) EmojiCategory.RECENT else EmojiCategory.SMILEYS)
    }

    val quickFilterKeywords = remember {
        listOf("senyum", "hati", "api", "ketawa", "jempol", "kucing", "kopi", "makan", "sedih", "pesta", "bintang", "doa", "mobil")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(totalKeyboardHeight)
            .background(bgColor)
    ) {
        if (!isSearchActive) {
            // ==========================================
            // MODE 1: STANDARD GBOARD EMOJI BROWSER
            // ==========================================

            // 1. Top Minimal Bar: Back | Search Pill | Backspace
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

                // Clean Search Bar Pill (Tap to open search mode)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(bgColor)
                        .clickable { isSearchActive = true }
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

            // 2. Middle Area: Spacious 8-Column Emoji Grid
            val displayedEmojis = remember(selectedCategory, recentEmojis) {
                if (selectedCategory == EmojiCategory.RECENT) {
                    recentEmojis
                } else {
                    EmojiProvider.getEmojisForCategory(selectedCategory)
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
        } else {
            // ==========================================
            // MODE 2: EMOJI SEARCH MODE (FOCUSED & FAST)
            // ==========================================

            // 1. Search Header: Back | Query Display | Clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(surfaceColor)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable {
                            isSearchActive = false
                            searchQuery = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_left),
                        contentDescription = "Kembali ke Kategori Emoji",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(bgColor)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) searchQuery else "Ketik untuk mencari...",
                            color = if (searchQuery.isNotEmpty()) textColor else textColor.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            fontWeight = if (searchQuery.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (searchQuery.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable { searchQuery = "" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Hapus Query",
                                    tint = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Delete Query Backspace
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (searchQuery.isNotEmpty()) {
                                searchQuery = searchQuery.dropLast(1)
                            } else {
                                onBackspaceClick()
                            }
                        },
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

            // 2. Quick Keyword Chips (One-tap filter)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(surfaceColor.copy(alpha = 0.7f))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                quickFilterKeywords.forEach { keyword ->
                    val isSelected = searchQuery.equals(keyword, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) accentColor else Color(theme.keyBackgroundColor).copy(alpha = 0.6f))
                            .clickable { searchQuery = keyword }
                            .padding(horizontal = 9.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = keyword.replaceFirstChar { it.uppercase() },
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White else textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 3. Search Results Grid (Real-time updates above keyboard)
            val searchResults = remember(searchQuery, recentEmojis) {
                if (searchQuery.isBlank()) {
                    if (recentEmojis.isNotEmpty()) recentEmojis else EmojiProvider.getEmojisForCategory(EmojiCategory.SMILEYS).take(16)
                } else {
                    EmojiProvider.search(searchQuery)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ditemukan emoji untuk '$searchQuery'",
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
                        items(searchResults, key = { it }) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onEmojiClick(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Built-in Search Typing Keyboard
            InlineEmojiSearchKeyboard(
                theme = theme,
                onCharClick = { char -> searchQuery += char },
                onBackspace = {
                    if (searchQuery.isNotEmpty()) {
                        searchQuery = searchQuery.dropLast(1)
                    }
                },
                onSpace = { searchQuery += " " },
                onClear = { searchQuery = "" },
                onClose = {
                    isSearchActive = false
                    searchQuery = ""
                }
            )
        }
    }
}

@Composable
private fun InlineEmojiSearchKeyboard(
    theme: SboardTheme,
    onCharClick: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit
) {
    val row1 = "qwertyuiop".toList()
    val row2 = "asdfghjkl".toList()
    val row3 = "zxcvbnm".toList()
    val textColor = Color(theme.keyTextColor)
    val keyBg = Color(theme.keyBackgroundColor)
    val accentColor = Color(theme.accentColor)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(theme.surfaceColor))
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Row 1
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            row1.forEach { char ->
                SearchKeyCell(char = char.toString(), weight = 1f, keyBg = keyBg, textColor = textColor) {
                    onCharClick(char)
                }
            }
        }

        // Row 2
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Spacer(modifier = Modifier.weight(0.5f))
            row2.forEach { char ->
                SearchKeyCell(char = char.toString(), weight = 1f, keyBg = keyBg, textColor = textColor) {
                    onCharClick(char)
                }
            }
            Spacer(modifier = Modifier.weight(0.5f))
        }

        // Row 3
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(keyBg.copy(alpha = 0.7f))
                    .clickable { onClear() },
                contentAlignment = Alignment.Center
            ) {
                Text("Bersihkan", fontSize = 10.sp, color = textColor)
            }

            row3.forEach { char ->
                SearchKeyCell(char = char.toString(), weight = 1f, keyBg = keyBg, textColor = textColor) {
                    onCharClick(char)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(keyBg)
                    .clickable { onBackspace() },
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

        // Row 4: Controls (Kembali, Spasi, Selesai)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(keyBg)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("Kembali", fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Bold)
            }

            Box(
                modifier = Modifier
                    .weight(4f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(keyBg)
                    .clickable { onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text("Spasi", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
            }

            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accentColor)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text("Selesai", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SearchKeyCell(
    char: String,
    weight: Float,
    keyBg: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .weight(weight)
            .height(34.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(keyBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char,
            fontSize = 14.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}
