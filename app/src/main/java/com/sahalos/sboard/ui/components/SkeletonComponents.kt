package com.sahalos.sboard.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

fun Modifier.shimmerEffect(shape: Shape = RoundedCornerShape(8.dp)): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                start = Offset(startOffsetX, 0f),
                end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
            )
        )
        .onGloballyPositioned { size = it.size }
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    Box(modifier = modifier.shimmerEffect(shape))
}

@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SkeletonBox(modifier = Modifier.height(20.dp).fillMaxWidth(0.6f))
            Spacer(modifier = Modifier.height(12.dp))
            SkeletonBox(modifier = Modifier.height(14.dp).fillMaxWidth(0.9f))
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(modifier = Modifier.height(14.dp).fillMaxWidth(0.4f))
        }
    }
}

@Composable
fun SkeletonListItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SkeletonBox(modifier = Modifier.size(40.dp), shape = RoundedCornerShape(8.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            SkeletonBox(modifier = Modifier.height(16.dp).fillMaxWidth(0.7f))
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonBox(modifier = Modifier.height(12.dp).fillMaxWidth(0.4f))
        }
    }
}
