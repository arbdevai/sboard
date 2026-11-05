package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.sahalos.sboard.model.ClipItem
import com.sahalos.sboard.model.SboardTheme

@Composable
fun ClipboardDrawer(
    clips: List<ClipItem>,
    theme: SboardTheme,
    onClipClick: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barBgColor = Color(theme.surfaceColor)
    val cardBgColor = Color(theme.keyBackgroundColor)
    val textColor = Color(theme.keyTextColor)
    val accentColor = Color(theme.accentColor)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barBgColor)
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Papan Klip",
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (clips.isNotEmpty()) {
                Text(
                    text = "Bersihkan",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onClearAll() }
                )
            }
        }

        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Papan klip kosong",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clips, key = { it.id }) { clip ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(cardBgColor)
                            .clickable { onClipClick(clip.text) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (clip.isSensitive) "••••••••" else clip.text,
                            color = textColor,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(110.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pin),
                            contentDescription = "Sematkan",
                            tint = if (clip.isPinned) accentColor else textColor.copy(alpha = 0.35f),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onTogglePin(clip.id) }
                        )
                    }
                }
            }
        }
    }
}
