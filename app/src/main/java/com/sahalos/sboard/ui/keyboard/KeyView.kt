package com.sahalos.sboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sahalos.sboard.R
import com.sahalos.sboard.model.KeyModel
import com.sahalos.sboard.model.KeyType
import com.sahalos.sboard.model.ModifierState
import com.sahalos.sboard.model.SboardTheme

@Composable
fun KeyView(
    key: KeyModel,
    modifierState: ModifierState,
    theme: SboardTheme,
    actionImeLabel: String? = null,
    actionIconRes: Int = R.drawable.ic_enter,
    enableKeyPopup: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isClickablePressed by interactionSource.collectIsPressedAsState()
    var isGesturePressed by remember { mutableStateOf(false) }
    val isPressed = isClickablePressed || isGesturePressed

    val shape = RoundedCornerShape(theme.keyCornerRadiusDp.dp)

    val keyBgColor = when (key.type) {
        KeyType.ACTION -> Color(theme.keyActionColor)
        KeyType.SHIFT -> if (modifierState.isShifted || modifierState.isCapsLock) Color(theme.accentColor) else Color(theme.keyBackgroundColor)
        KeyType.CTRL -> if (modifierState.isCtrlActive) Color(theme.accentColor) else Color(theme.keyBackgroundColor)
        KeyType.FN -> if (modifierState.isFnActive) Color(theme.accentColor) else Color(theme.keyBackgroundColor)
        else -> Color(theme.keyBackgroundColor)
    }

    val textColor = when (key.type) {
        KeyType.ACTION -> if (theme.isDark) Color(0xFF1E293B) else Color.White
        else -> Color(theme.keyTextColor)
    }

    val interactiveModifier = when {
        onClick != null && onLongClick != null -> {
            Modifier.pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        isGesturePressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isGesturePressed = false
                        }
                    },
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
        }
        onClick != null -> {
            Modifier.clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = textColor.copy(alpha = 0.2f)),
                onClick = onClick
            )
        }
        else -> Modifier
    }

    // Outer container without clipping, allowing floating popup to overflow upwards
    Box(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        // Physical key surface (clipped with rounded corners)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(theme.keyElevationDp.dp, shape = shape)
                .clip(shape)
                .background(keyBgColor)
                .then(interactiveModifier),
            contentAlignment = Alignment.Center
        ) {
            // Secondary label on top-right (e.g. symbols / numbers)
            if (!key.secondaryLabel.isNullOrEmpty() && key.type == KeyType.CHARACTER) {
                Text(
                    text = key.secondaryLabel,
                    color = Color(theme.keySecondaryTextColor).copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 4.dp, top = 2.dp)
                )
            }

            // Primary content
            when (key.type) {
                KeyType.SHIFT -> {
                    val iconRes = if (modifierState.isCapsLock) R.drawable.ic_caps_lock else R.drawable.ic_shift
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = "Shift",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                KeyType.BACKSPACE -> {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_backspace),
                        contentDescription = "Backspace",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                KeyType.ACTION -> {
                    if (actionImeLabel != null) {
                        Text(
                            text = actionImeLabel,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = actionIconRes),
                            contentDescription = "Action",
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                KeyType.SPACE -> {
                    Text(
                        text = "Sboard",
                        color = textColor.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                else -> {
                    val displayText = if (modifierState.isShifted || modifierState.isCapsLock) {
                        key.primaryLabel.uppercase()
                    } else {
                        key.primaryLabel
                    }
                    Text(
                        text = displayText,
                        color = textColor,
                        fontSize = if (displayText.length > 2) 12.sp else 16.sp,
                        fontWeight = if (key.type == KeyType.CHARACTER) FontWeight.Normal else FontWeight.Medium
                    )
                }
            }
        }

        // Floating Key Magnifier Popup (Active on touch down)
        if (isPressed && enableKeyPopup && key.type == KeyType.CHARACTER) {
            val popupChar = if (modifierState.isShifted || modifierState.isCapsLock) {
                key.primaryLabel.uppercase()
            } else {
                key.primaryLabel
            }
            FloatingKeyBubble(
                label = popupChar,
                secondaryLabel = key.secondaryLabel,
                theme = theme,
                modifier = Modifier
                    .offset(y = (-52).dp)
                    .zIndex(100f)
            )
        }
    }
}

@Composable
private fun FloatingKeyBubble(
    label: String,
    secondaryLabel: String?,
    theme: SboardTheme,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(10.dp)
    val bubbleBg = Color(theme.keyBackgroundColor)
    val textColor = Color(theme.keyTextColor)
    val accentColor = Color(theme.accentColor)

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 54.dp)
            .shadow(8.dp, shape = bubbleShape)
            .clip(bubbleShape)
            .background(bubbleBg)
            .border(1.dp, accentColor.copy(alpha = 0.35f), bubbleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!secondaryLabel.isNullOrEmpty()) {
            Text(
                text = secondaryLabel,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 5.dp)
            )
        }
        Text(
            text = label,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}
