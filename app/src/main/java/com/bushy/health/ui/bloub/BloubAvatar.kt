package com.bushy.health.ui.bloub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import com.bushy.health.AvatarExpression
import com.bushy.health.ThemeMode
import com.bushy.health.VisualStyle

@Composable
fun BloubAvatar(
    expression: AvatarExpression,
    modifier: Modifier = Modifier,
    visualStyle: VisualStyle = VisualStyle.MATERIAL3,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    externalTouchPositionInWindow: Offset? = null,
    onClick: (() -> Unit)? = null
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val isDark = remember(themeMode, systemInDarkTheme) {
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemInDarkTheme
        }
    }

    val bodyColor = when {
        visualStyle == VisualStyle.MONOCHROME && isDark -> Color.White
        visualStyle == VisualStyle.MONOCHROME && !isDark -> Color.Black
        else -> MaterialTheme.colorScheme.primary
    }

    val eyeColor = when {
        visualStyle == VisualStyle.MONOCHROME && isDark -> Color.Black
        visualStyle == VisualStyle.MONOCHROME && !isDark -> Color.White
        else -> MaterialTheme.colorScheme.onPrimary
    }

    val engine = remember { BloubEngine() }

    var timeInSeconds by remember { mutableFloatStateOf(0f) }
    var localTouchOffset by remember { mutableStateOf<Offset?>(null) }
    var avatarCenterInWindow by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(Unit) {
        val startTime = System.nanoTime()
        while (true) {
            withFrameNanos { frameNanos ->
                timeInSeconds = (frameNanos - startTime) / 1_000_000_000f
            }
        }
    }

    LaunchedEffect(expression, timeInSeconds) {
        engine.updateExpression(expression, timeInSeconds)
    }

    val baseModifier = if (onClick != null) modifier.clickable { onClick() } else modifier

    Box(
        modifier = baseModifier
            .onGloballyPositioned { coordinates ->
                avatarCenterInWindow = coordinates.boundsInWindow().center
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val position = event.changes.firstOrNull()?.position
                        val pressed = event.changes.any { it.pressed }
                        localTouchOffset = if (pressed && position != null) position else null
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = minOf(size.width, size.height) * 0.44f

            val targetTouchGaze = when {
                externalTouchPositionInWindow != null && avatarCenterInWindow != null -> {
                    val extPos = externalTouchPositionInWindow
                    val avatarCenter = avatarCenterInWindow!!
                    val dx = ((extPos.x - avatarCenter.x) / (baseRadius * 1.5f)).coerceIn(-1.8f, 1.8f)
                    val dy = ((extPos.y - avatarCenter.y) / (baseRadius * 1.5f)).coerceIn(-1.8f, 1.8f)
                    HeadGaze(
                        yaw = dx * 42f,
                        pitch = -dy * 38f,
                        roll = dx * 12f
                    )
                }
                localTouchOffset != null -> {
                    val pos = localTouchOffset!!
                    val dx = ((pos.x - centerOffset.x) / baseRadius).coerceIn(-1.6f, 1.6f)
                    val dy = ((pos.y - centerOffset.y) / baseRadius).coerceIn(-1.6f, 1.6f)
                    HeadGaze(
                        yaw = dx * 42f,
                        pitch = -dy * 38f,
                        roll = dx * 12f
                    )
                }
                else -> null
            }

            val frame = engine.sampleFrame(
                timeInSeconds = timeInSeconds,
                center = centerOffset,
                baseRadius = baseRadius,
                targetTouchGaze = targetTouchGaze
            )

            drawPath(
                path = frame.bodyPath,
                color = bodyColor,
                alpha = frame.bodyAlpha
            )

            for (eye in frame.eyes) {
                drawPath(
                    path = eye.path,
                    color = eyeColor,
                    alpha = frame.eyeAlpha
                )
            }
        }
    }
}
