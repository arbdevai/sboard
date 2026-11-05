package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    val textColor = Color(theme.keyTextColor)
    val accentColor = Color(theme.accentColor)

    val quickFilters = listOf("senyum", "hati", "api", "ketawa", "jempol", "kucing", "kopi", "makan", "sedih", "pesta", "bintang", "doa")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBg)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Row 1: Search Header (Back Arrow | Query Display | Clear Button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Exit Search Mode
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable { onCloseSearch() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_left),
                    contentDescription = "Kembali ke Keyboard",
                    tint = textColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Query Display Pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(theme.backgroundColor))
                    .padding(horizontal = 8.dp),
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
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (query.isNotEmpty()) query else "Ketik untuk mencari emoji...",
                        color = if (query.isNotEmpty()) textColor else textColor.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontWeight = if (query.isNotEmpty()) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { onClearQuery() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Bersihkan",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }

        // Row 2: Real-time Live Matching Emoji Strip or Quick Keyword Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (results.isNotEmpty()) {
                results.take(30).forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onEmojiClick(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 20.sp
                        )
                    }
                }
            } else if (query.isBlank()) {
                // Show Quick Filter Chips when no query typed yet
                quickFilters.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(theme.keyBackgroundColor).copy(alpha = 0.6f))
                            .clickable { onQuickFilterClick(tag) }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tag.replaceFirstChar { it.uppercase() },
                            fontSize = 10.sp,
                            color = textColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Text(
                    text = "Tidak ada emoji untuk '$query'",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
