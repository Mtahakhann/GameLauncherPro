package com.tahalabs.gamelauncherpro.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val ring = remember { Animatable(0f) }
    val letters = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }

    val glow by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    LaunchedEffect(Unit) {
        launch { ring.animateTo(1f, tween(1500, easing = FastOutSlowInEasing)) }
        launch {
            delay(600)
            letters.animateTo(1f, tween(1600, easing = LinearEasing))
        }
        bar.animateTo(1f, tween(3000, easing = FastOutSlowInEasing))
        delay(350)
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Bg)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(Pal.Accent.copy(alpha = 0.10f * glow), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.38f),
                        radius = size.width * 0.95f
                    )
                )
            }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 36.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1.25f))

            Emblem(progress = ring.value, modifier = Modifier.size(104.dp))
            Spacer(Modifier.height(26.dp))
            Text(
                "Game Launcher Pro",
                color = Pal.Text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.5.sp,
                modifier = Modifier.graphicsLayer { alpha = ring.value }
            )

            Spacer(Modifier.weight(1f))

            Text(
                "developed by",
                color = Pal.Faint,
                fontSize = 11.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer { alpha = (letters.value * 2f).coerceAtMost(1f) }
            )
            Spacer(Modifier.height(10.dp))
            Wordmark(progress = letters.value)

            Spacer(Modifier.height(44.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Pal.Line)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(bar.value)
                        .fillMaxHeight()
                        .background(Pal.Accent)
                )
            }
        }
    }
}

/** TAHALABS — thin "TAHA", bold mint "LABS", letters rise in one by one. */
@Composable
private fun Wordmark(progress: Float) {
    val word = "TAHALABS"
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        word.forEachIndexed { i, ch ->
            val p = ((progress * (word.length + 4) - i) / 4f).coerceIn(0f, 1f)
            val first = i < 4
            Text(
                text = ch.toString(),
                color = if (first) Pal.Text else Pal.Accent,
                fontSize = 36.sp,
                fontWeight = if (first) FontWeight.ExtraLight else FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    alpha = p
                    translationY = (1f - p) * 20.dp.toPx()
                }
            )
        }
    }
}
