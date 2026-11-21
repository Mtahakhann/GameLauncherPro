package com.tahalabs.gamelauncherpro.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tahalabs.gamelauncherpro.engine.Perms

@Composable
fun SetupScreen(perms: Perms) {
    val ctx = LocalContext.current
    val adbCommand =
        "adb shell pm grant ${ctx.packageName} android.permission.WRITE_SECURE_SETTINGS"

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text("Setup", color = Pal.Text, fontSize = 32.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(2.dp))
            Text("Grant these once. Everything else works straight away.", color = Pal.Dim, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))

        SetupRow(
            title = "Display over other apps",
            desc = "Lets the live HUD sit on top of your game.",
            granted = perms.overlay,
            action = "Grant"
        ) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
            )
        }

        SetupRow(
            title = "Do Not Disturb access",
            desc = "Mutes notifications and calls from strangers during play.",
            granted = perms.dnd,
            action = "Grant"
        ) {
            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }

        SetupRow(
            title = "Notifications",
            desc = "Shows the session card with an End session button.",
            granted = perms.notif,
            action = "Allow"
        ) {
            ctx.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            )
        }

        Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Advanced tuning", color = Pal.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Unlocks Lift power limits and Instant system animations.",
                        color = Pal.Dim,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                if (perms.secure) StatusChip("Ready")
            }
            if (!perms.secure) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Turn on USB debugging, plug the phone into a computer once, then run this command. " +
                        "It survives reboots, and you can unplug afterwards.",
                    color = Pal.Dim,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    adbCommand,
                    color = Pal.Accent,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Pal.Bg)
                        .padding(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                PillButton("Copy command", compact = true) {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("adb", adbCommand))
                    Toast.makeText(ctx, "Command copied", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Panel(Modifier.fillMaxWidth()) {
            Text("What boosting can and can't do", color = Pal.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Android doesn't let normal apps change another app's CPU or GPU priority; that takes root. " +
                    "The game you open already gets top scheduling priority as the foreground app. " +
                    "Game Launcher Pro protects that advantage: it frees RAM, silences interruptions, " +
                    "lifts Battery Saver throttling and shows you the temperature so you can see when " +
                    "the phone starts to heat-throttle.",
                color = Pal.Dim,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "TAHALABS  \u2022  Game Launcher Pro 1.0",
            color = Pal.Faint,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SetupRow(
    title: String,
    desc: String,
    granted: Boolean,
    action: String,
    onClick: () -> Unit
) {
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Pal.Text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(desc, color = Pal.Dim, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            if (granted) StatusChip("Ready") else PillButton(action, compact = true, onClick = onClick)
        }
    }
}
