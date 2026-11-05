package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahalos.sboard.R
import com.sahalos.sboard.model.ClipItem
import com.sahalos.sboard.model.SboardTheme

@Composable
fun ClipboardScreen(
    clips: List<ClipItem>,
    theme: SboardTheme,
    totalKeyboardHeight: Dp,
    onClipClick: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    onDeleteClip: (String) -> Unit,
    onClearAll: () -> Unit,
    onBackToKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedClipForAction by remember { mutableStateOf<ClipItem?>(null) }

    val bgColor = Color(theme.backgroundColor)
    val surfaceColor = Color(theme.surfaceColor)
    val cardColor = Color(theme.keyBackgroundColor)
    val textColor = Color(theme.keyTextColor)
    val accentColor = Color(theme.accentColor)

    val pinnedClips = clips.filter { it.isPinned }
    val unpinnedClips = clips.filter { !it.isPinned }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalKeyboardHeight)
            .background(bgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Gboard-style Top Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(surfaceColor)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onBackToKeyboard() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_left),
                        contentDescription = "Kembali ke Keyboard",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Papan Klip",
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (unpinnedClips.isNotEmpty()) {
                    Text(
                        text = "Hapus Semua",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onClearAll() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Content: 2-Column Grid of Clips
            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Papan klip kosong.\nTeks yang disalin akan muncul di sini.",
                        color = textColor.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (pinnedClips.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "DISEMATKAN",
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 2.dp)
                            )
                        }
                        items(pinnedClips, key = { it.id }) { clip ->
                            ClipGridCard(
                                clip = clip,
                                cardColor = cardColor,
                                textColor = textColor,
                                accentColor = accentColor,
                                onClick = { onClipClick(clip.text) },
                                onLongClick = { selectedClipForAction = clip }
                            )
                        }
                    }

                    if (unpinnedClips.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = "TERBARU",
                                color = textColor.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp, top = 4.dp, bottom = 2.dp)
                            )
                        }
                        items(unpinnedClips, key = { it.id }) { clip ->
                            ClipGridCard(
                                clip = clip,
                                cardColor = cardColor,
                                textColor = textColor,
                                accentColor = accentColor,
                                onClick = { onClipClick(clip.text) },
                                onLongClick = { selectedClipForAction = clip }
                            )
                        }
                    }
                }
            }
        }

        // Safe Inline Modal Overlay (No AlertDialog, Zero BadTokenException in IME Service)
        selectedClipForAction?.let { clip ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { selectedClipForAction = null },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clickable(enabled = false) {}, // prevent dismissing when tapping card
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Opsi Klip",
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Tutup",
                                tint = textColor.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { selectedClipForAction = null }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (clip.isSensitive) "•••••••• (Data Sensitif)" else clip.text,
                            color = textColor.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = textColor.copy(alpha = 0.12f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pin / Unpin Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardColor)
                                    .clickable {
                                        onTogglePin(clip.id)
                                        selectedClipForAction = null
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pin),
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (clip.isPinned) "Lepas" else "Sematkan",
                                    color = textColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Delete Button
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                    .clickable {
                                        onDeleteClip(clip.id)
                                        selectedClipForAction = null
                                    }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hapus",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipGridCard(
    clip: ClipItem,
    cardColor: Color,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(1.dp, shape = shape)
            .clip(shape)
            .background(cardColor)
            .pointerInput(clip.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (clip.isSensitive) "•••••••• (Data Sensitif)" else clip.text,
            color = textColor,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        if (clip.isPinned) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pin),
                contentDescription = "Disematkan",
                tint = accentColor,
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}
