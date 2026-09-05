package com.fitlog.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitlog.app.ui.theme.Volt
import com.fitlog.app.ui.theme.VoltDeep
import com.fitlog.app.ui.theme.VoltInk

/** 按压缩放 + 触觉反馈（替代默认水波纹的"实体按压"手感） */
fun Modifier.pressClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "pressScale"
    )
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(pressed) {
        if (pressed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

/** Volt 主按钮：荧光绿渐变 + 顶部内高光 + 同色泛光阴影 */
@Composable
fun VoltButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 54.dp
) {
    val shape = RoundedCornerShape(16.dp)
    val bg = if (enabled) Brush.verticalGradient(listOf(Volt, VoltDeep))
    else Brush.verticalGradient(
        listOf(Volt.copy(alpha = 0.35f), VoltDeep.copy(alpha = 0.35f))
    )
    Box(
        modifier = modifier
            .height(height)
            .shadow(16.dp, shape, ambientColor = Volt.copy(alpha = 0.30f), spotColor = Volt.copy(alpha = 0.40f))
            .clip(shape)
            .background(bg)
            .pressClick(enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 顶部内高光，做出实体按键的厚度
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                        startY = 0f, endY = 90f
                    )
                )
        )
        Text(text, color = VoltInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

/** 次级/危险操作按钮：实底厚边框 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.error,
    height: Dp = 54.dp
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .pressClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** 厚卡：实底 + 粗描边 + 18dp 圆角（替代轻薄 Card） */
@Composable
fun ThickCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(color)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    ) { content() }
}

/** 环形进度（今日环/休息倒数） */
@Composable
fun ProgressRing(
    progress: Float,
    size: Dp,
    modifier: Modifier = Modifier,
    stroke: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable () -> Unit = {}
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                style = Stroke(s, cap = StrokeCap.Round)
            )
            drawArc(
                color = color, startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
                style = Stroke(s, cap = StrokeCap.Round)
            )
        }
        content()
    }
}

/** 让大数字使用等宽字形（重量/次数/计时不跳宽） */
val TabularNumbers = androidx.compose.ui.text.TextStyle(
    fontFeatureSettings = "tnum"
)
