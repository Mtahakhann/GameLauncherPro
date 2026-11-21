package com.tahalabs.gamelauncherpro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tahalabs.gamelauncherpro.data.AppEntry
import com.tahalabs.gamelauncherpro.data.BoostSettings
import com.tahalabs.gamelauncherpro.engine.Booster
import com.tahalabs.gamelauncherpro.engine.Perms
import com.tahalabs.gamelauncherpro.service.GameHudService
import kotlinx.coroutines.delay

enum class StepState { Pending, Running, Done, Skipped }

@Stable
class Step(val title: String) {
    var state by mutableStateOf(StepState.Pending)
    var detail by mutableStateOf("")
}

/** Full-screen "preparing your game" sequence. Every step does real work, then the game opens. */
@Composable
fun BoostSessionScreen(
    game: AppEntry,
    settings: BoostSettings,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val ctx = LocalContext.current
    val steps = remember {
        listOf(
            Step("Checking memory"),
            Step("Clearing background apps"),
            Step("Silencing interruptions"),
            Step("Lifting power limits"),
            Step("Speeding up system animations"),
            Step("Starting the live session"),
            Step("Opening ${game.label}")
        )
    }

    BackHandler {
        Booster.restoreSession(ctx)
        GameHudService.stop(ctx)
        onCancel()
    }

    LaunchedEffect(game.pkg) {
        val perms = Perms.read(ctx)

        suspend fun doStep(s: Step, skip: String?, work: suspend () -> String) {
            s.state = StepState.Running
            delay(320)
            if (skip != null) {
                s.detail = skip
                s.state = StepState.Skipped
            } else {
                s.detail = work()
                s.state = StepState.Done
            }
            delay(120)
        }

        doStep(steps[0], null) {
            val m = Booster.memory(ctx)
            "${Booster.gb(m.availMb)} GB free of ${Booster.gb(m.totalMb)} GB"
        }

        doStep(steps[1], if (!settings[BoostSettings.CLEAR_RAM]) "Turned off" else null) {
            val r = Booster.clearRam(ctx, setOf(game.pkg))
            if (r.freedMb > 0) "${r.freedMb} MB freed from ${r.swept} apps"
            else "Already lean (${r.swept} apps checked)"
        }

        doStep(
            steps[2],
            when {
                !settings[BoostSettings.DND] -> "Turned off"
                !perms.dnd -> "Needs Do Not Disturb access (Setup)"
                else -> null
            }
        ) {
            if (Booster.enableDnd(ctx)) "Priority-only mode on" else "Couldn't change Do Not Disturb"
        }

        doStep(
            steps[3],
            when {
                !settings[BoostSettings.LIFT_LIMITS] -> "Turned off"
                !perms.secure -> "Needs the one-time ADB step (Setup)"
                else -> null
            }
        ) {
            when (Booster.disableBatterySaver(ctx)) {
                true -> "Battery Saver switched off"
                false -> "Battery Saver was already off"
                null -> "Couldn't change the setting"
            }
        }

        doStep(
            steps[4],
            when {
                !settings[BoostSettings.ANIMATIONS] -> "Turned off"
                !perms.secure -> "Needs the one-time ADB step (Setup)"
                else -> null
            }
        ) {
            if (Booster.speedUpAnimations(ctx)) "Animations at 0.5x" else "Couldn't change the setting"
        }

        doStep(steps[5], null) {
            val hudOn = settings[BoostSettings.HUD] && perms.overlay
            val guardOn = settings[BoostSettings.GUARD]
            GameHudService.start(ctx, game.pkg, game.label, hudOn, guardOn)
            val parts = ArrayList<String>()
            if (hudOn) parts.add("HUD on")
            if (settings[BoostSettings.HUD] && !perms.overlay) parts.add("HUD needs overlay access")
            if (guardOn) parts.add("Live Guard on")
            if (parts.isEmpty()) "Session tracking only" else parts.joinToString(", ")
        }

        doStep(steps[6], null) {
            val intent = ctx.packageManager.getLaunchIntentForPackage(game.pkg)
            if (intent != null) {
                ctx.startActivity(intent)
                "Good luck"
            } else {
                GameHudService.stop(ctx)
                Booster.restoreSession(ctx)
                "Couldn't open this game. Is it still installed?"
            }
        }
        delay(400)
        onDone()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Pal.Bg)
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(28.dp))

        val pulse = rememberInfiniteTransition(label = "pulse")
        val scale: Float by pulse.animateFloat(
            initialValue = 1f,
            targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
            label = "pulseScale"
        )
        val ringAlpha: Float by pulse.animateFloat(
            initialValue = 0.55f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
            label = "pulseAlpha"
        )

        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(92.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = ringAlpha
                    }
                    .border(2.dp, Pal.Accent, RoundedCornerShape(26.dp))
            )
            val icon = game.icon
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Getting ready", color = Pal.Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(
            game.label,
            color = Pal.Text,
            fontSize = 26.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Column(Modifier.fillMaxWidth()) {
            steps.forEach { StepRow(it) }
        }

        Spacer(Modifier.weight(1f))
        Text("Press back to cancel", color = Pal.Faint, fontSize = 12.sp)
    }
}

@Composable
private fun StepRow(s: Step) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.Top
    ) {
        StepDot(s.state)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                s.title,
                color = if (s.state == StepState.Pending) Pal.Faint else Pal.Text,
                fontSize = 15.sp
            )
            if (s.detail.isNotEmpty()) {
                Text(
                    s.detail,
                    color = if (s.state == StepState.Skipped) Pal.Warn else Pal.Dim,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StepDot(state: StepState) {
    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        when (state) {
            StepState.Pending -> Box(
                Modifier
                    .size(14.dp)
                    .border(1.5.dp, Pal.Line, CircleShape)
            )
            StepState.Running -> {
                val dotTransition = rememberInfiniteTransition(label = "dot")
                val a: Float by dotTransition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
                    label = "dotAlpha"
                )
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Pal.Accent.copy(alpha = a))
                )
            }
            StepState.Done -> Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Pal.Accent),
                contentAlignment = Alignment.Center
            ) {
                Text("\u2713", color = Pal.Bg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            StepState.Skipped -> Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .border(1.5.dp, Pal.Warn.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("\u2013", color = Pal.Warn, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}