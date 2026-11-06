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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahalos.sboard.R
import com.sahalos.sboard.model.SboardTheme

@Composable
fun EmojiSearchToolbar(
    query: String,
    results: List<String>,
    theme: SboardTheme,
    onEmojiClick: (String) -> Unit,
    onClearQuery: () -> Unit,
    onCloseSearch: () -> Unit,
    onQuickFilterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val barBg = Color(theme.surfaceColor)
    val bgColor = Color(theme.backgroundColor)
    val textColor = Color(theme.keyTextColor)
    val accentColor = Color(theme.accentColor)

    val quickFilters = listOf(
        "senyum", "ketawa", "hati", "api", "kucing", "anjing",
        "mobil", "makan", "sedih", "jempol", "doa", "bintang",
        "kopi", "bunga", "musik", "indonesia"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
    ) {
        // 1. Search Header (Back Arrow | Query Display Pill | Clear Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exit Search Mode back to keyboard
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onCloseSearch() },
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

            // Query Display Pill
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (query.isNotEmpty()) query else "Cari dari 1.800+ emoji...",
                        color = if (query.isNotEmpty()) textColor else textColor.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                        fontWeight = if (query.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onClearQuery() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Bersihkan",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Results Body: Gboard-style Vertical Scrollable Grid (Height = 115.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .background(bgColor)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (results.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(results, key = { it }) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onEmojiClick(emoji) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 21.sp
                            )
                        }
                    }
                }
            } else if (query.isBlank()) {
                // When no query is typed, show quick filter chips and helpful hint
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickFilters.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(barBg)
                                    .clickable { onQuickFilterClick(tag) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tag.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Text(
                        text = "Ketik kata kunci pada keyboard di bawah untuk mencari emoji",
                        color = textColor.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = "Tidak ada emoji untuk '$query'",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
