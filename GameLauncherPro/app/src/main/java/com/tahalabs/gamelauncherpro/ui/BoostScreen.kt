package com.tahalabs.gamelauncherpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tahalabs.gamelauncherpro.data.BoostSettings
import com.tahalabs.gamelauncherpro.engine.Booster
import com.tahalabs.gamelauncherpro.engine.Perms
import kotlinx.coroutines.launch

@Composable
fun BoostScreen(
    live: Live,
    settings: BoostSettings,
    perms: Perms,
    onEndSession: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Booster.TrimResult?>(null) }
    val m = live.mem
    val b = live.batt

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("Boost", color = Pal.Text, fontSize = 32.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(2.dp))
        Text("Free up your phone before you play.", color = Pal.Dim, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        live.session?.let { name ->
            SessionCard(name, onEndSession)
            Spacer(Modifier.height(20.dp))
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            RamGauge(
                fraction = m.usedFraction,
                big = "${(m.usedFraction * 100).toInt()}%",
                small = "${Booster.gb(m.usedMb)} of ${Booster.gb(m.totalMb)} GB in use",
                modifier = Modifier.size(224.dp)
            )
        }
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val tempColor = when {
                b.tempC >= 44f -> Pal.Hot
                b.tempC >= 40f -> Pal.Warn
                else -> Pal.Text
            }
            StatCard("Battery temp", "${b.tempC.toInt()}\u00B0C", tempColor, Modifier.weight(1f))
            StatCard(
                "Battery",
                if (b.charging) "${b.level}% charging" else "${b.level}%",
                Pal.Text,
                Modifier.weight(1f)
            )
            StatCard("Storage free", "${live.storageFreeGb} GB", Pal.Text, Modifier.weight(1f))
        }
        Spacer(Modifier.height(22.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PillButton(if (busy) "Optimising\u2026" else "Boost now") {
                if (!busy) {
                    busy = true
                    scope.launch {
                        result = Booster.clearRam(ctx, emptySet())
                        busy = false
                    }
                }
            }
        }
        result?.let { r ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (r.freedMb > 0) "Freed ${r.freedMb} MB from ${r.swept} apps"
                else "Already lean. Nothing left to free.",
                color = if (r.freedMb > 0) Pal.Accent else Pal.Dim,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(30.dp))
        SectionLabel("What a boost does")
        Spacer(Modifier.height(10.dp))
        Panel(Modifier.fillMaxWidth()) {
            ToggleRow(
                "Clear RAM",
                "Sweeps background apps right before your game opens.",
                settings[BoostSettings.CLEAR_RAM]
            ) { settings[BoostSettings.CLEAR_RAM] = it }
            Hairline()
            ToggleRow(
                "Silence interruptions",
                "Priority-only Do Not Disturb while you play, restored afterwards.",
                settings[BoostSettings.DND],
                needs = if (!perms.dnd) "Needs Do Not Disturb access. See Setup." else null
            ) { settings[BoostSettings.DND] = it }
            Hairline()
            ToggleRow(
                "Lift power limits",
                "Turns Battery Saver off so Android stops throttling the CPU.",
                settings[BoostSettings.LIFT_LIMITS],
                needs = if (!perms.secure) "Needs the one-time ADB step. See Setup." else null
            ) { settings[BoostSettings.LIFT_LIMITS] = it }
            Hairline()
            ToggleRow(
                "Instant system animations",
                "Halves animation time while playing. Affects the system UI only.",
                settings[BoostSettings.ANIMATIONS],
                needs = if (!perms.secure) "Needs the one-time ADB step. See Setup." else null
            ) { settings[BoostSettings.ANIMATIONS] = it }
            Hairline()
            ToggleRow(
                "Live HUD",
                "Small overlay with free RAM, temperature, battery and thermal state.",
                settings[BoostSettings.HUD],
                needs = if (!perms.overlay) "Needs Display over other apps. See Setup." else null
            ) { settings[BoostSettings.HUD] = it }
            Hairline()
            ToggleRow(
                "Live Guard",
                "Re-sweeps background apps every minute while your game runs.",
                settings[BoostSettings.GUARD]
            ) { settings[BoostSettings.GUARD] = it }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Panel(modifier, padding = 14.dp) {
        Text(label, color = Pal.Dim, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SessionCard(name: String, onEnd: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.Accent.copy(alpha = 0.08f))
            .border(1.dp, Pal.Accent.copy(alpha = 0.45f), shape)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Session active", color = Pal.Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(name, color = Pal.Text, fontSize = 16.sp)
        }
        PillButton("End session", filled = false, compact = true, onClick = onEnd)
    }
}
