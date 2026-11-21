package com.tahalabs.gamelauncherpro.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Pal.Dim) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = 13.sp,
        letterSpacing = 0.4.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(Pal.Surface)
            .border(1.dp, Pal.Line, shape)
            .padding(padding),
        content = content
    )
}

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (filled) Modifier.background(Pal.Accent)
                else Modifier.border(1.dp, Pal.Line, shape)
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) 16.dp else 26.dp,
                vertical = if (compact) 9.dp else 14.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (filled) Pal.Bg else Pal.Text,
            fontSize = if (compact) 13.sp else 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StatusChip(text: String, color: Color = Pal.Accent) {
    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, color.copy(alpha = 0.45f), shape)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

@Composable
fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Pal.Line)
    )
}

@Composable
fun LinearMeter(fraction: Float, modifier: Modifier = Modifier, color: Color = Pal.Accent) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "meter")
    Box(
        modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Pal.Raised)
    ) {
        Box(
            Modifier
                .fillMaxWidth(f)
                .fillMaxHeight()
                .background(color)
        )
    }
}

@Composable
fun RamGauge(fraction: Float, big: String, small: String, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        tween(900, easing = FastOutSlowInEasing),
        label = "gauge"
    )
    val tint = when {
        f > 0.88f -> Pal.Hot
        f > 0.75f -> Pal.Warn
        else -> Pal.Accent
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12.dp.toPx()
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = Pal.Raised,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (f > 0.005f) {
                drawArc(
                    color = tint,
                    startAngle = 135f,
                    sweepAngle = 270f * f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(big, color = Pal.Text, fontSize = 44.sp, fontWeight = FontWeight.ExtraLight)
            Text(small, color = Pal.Dim, fontSize = 13.sp)
        }
    }
}

/** The Game Launcher Pro mark: a ring and a bolt. [progress] draws the ring in. */
@Composable
fun Emblem(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val stroke = s * 0.028f
        val r = s * 0.46f
        val c = Offset(s / 2f, s / 2f)
        drawCircle(color = Pal.Line, radius = r, center = c, style = Stroke(width = stroke))
        drawArc(
            color = Pal.Accent,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(2f * r, 2f * r),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        val bolt = Path().apply {
            moveTo(0.56f * s, 0.24f * s)
            lineTo(0.38f * s, 0.54f * s)
            lineTo(0.49f * s, 0.54f * s)
            lineTo(0.44f * s, 0.78f * s)
            lineTo(0.64f * s, 0.45f * s)
            lineTo(0.53f * s, 0.45f * s)
            close()
        }
        drawPath(bolt, Pal.Accent.copy(alpha = progress.coerceIn(0f, 1f)))
    }
}

@Composable
fun ToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    needs: String? = null,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Pal.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(desc, color = Pal.Dim, fontSize = 12.sp, lineHeight = 16.sp)
            if (needs != null) {
                Spacer(Modifier.height(4.dp))
                Text(needs, color = Pal.Warn, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Pal.Bg,
                checkedTrackColor = Pal.Accent,
                checkedBorderColor = Pal.Accent,
                uncheckedThumbColor = Pal.Dim,
                uncheckedTrackColor = Pal.Raised,
                uncheckedBorderColor = Pal.Line
            )
        )
    }
}

@Composable
fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("Games", "Boost", "Setup")
    Column(
        Modifier
            .fillMaxWidth()
            .background(Pal.Bg)
            .navigationBarsPadding()
    ) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            labels.forEachIndexed { i, label ->
                val on = i == selected
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        label,
                        color = if (on) Pal.Text else Pal.Faint,
                        fontSize = 14.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (on) Pal.Accent else Color.Transparent)
                    )
                }
            }
        }
    }
}
