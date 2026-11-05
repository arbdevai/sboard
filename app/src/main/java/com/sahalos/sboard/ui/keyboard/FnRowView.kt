package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahalos.sboard.core.KeyLayoutProvider
import com.sahalos.sboard.model.SboardTheme

@Composable
fun FnRowView(
    theme: SboardTheme,
    onFunctionKeyClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val fnKeys = KeyLayoutProvider.getFunctionKeysRow()
    val barBgColor = Color(theme.surfaceColor)
    val keyBgColor = Color(theme.keyBackgroundColor)
    val textColor = Color(theme.keyTextColor)
    val accentColor = Color(theme.accentColor)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(barBgColor)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        fnKeys.forEach { key ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .shadow(1.dp, shape = RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(keyBgColor)
                    .clickable { onFunctionKeyClick(key.code) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key.primaryLabel,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
