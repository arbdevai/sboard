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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahalos.sboard.R
import com.sahalos.sboard.model.ClipItem
import com.sahalos.sboard.model.SboardTheme

@Composable
fun ClipboardScreen(
    clips: List<ClipItem>,
    theme: SboardTheme,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Gboard-style Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
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
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Papan Klip",
                    color = textColor,
                    fontSize = 14.sp,
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

        // Content List
        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Papan klip Anda kosong.\nTeks yang disalin akan tersimpan di sini.",
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pinnedClips.isNotEmpty()) {
                    item {
                        Text(
                            text = "DISEMATKAN",
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(pinnedClips, key = { it.id }) { clip ->
                        ClipCardItem(
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
                    item {
                        Text(
                            text = "TERBARU",
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                        )
                    }
                    items(unpinnedClips, key = { it.id }) { clip ->
                        ClipCardItem(
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

    // Contextual Action Dialog for Long-Pressed Card (Pin/Unpin & Delete)
    selectedClipForAction?.let { clip ->
        AlertDialog(
            onDismissRequest = { selectedClipForAction = null },
            title = {
                Text(
                    text = "Opsi Klip",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (clip.isSensitive) "••••••••" else clip.text,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            onTogglePin(clip.id)
                            selectedClipForAction = null
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pin),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (clip.isPinned) "Lepas Sematan" else "Sematkan")
                    }
                    TextButton(
                        onClick = {
                            onDeleteClip(clip.id)
                            selectedClipForAction = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedClipForAction = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun ClipCardItem(
    clip: ClipItem,
    cardColor: Color,
    textColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, shape = shape)
            .clip(shape)
            .background(cardColor)
            .pointerInput(clip.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (clip.isSensitive) "•••••••• (Data Sensitif)" else clip.text,
            color = textColor,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (clip.isPinned) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_pin),
                contentDescription = "Disematkan",
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
