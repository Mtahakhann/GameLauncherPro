package com.tahalabs.gamelauncherpro.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tahalabs.gamelauncherpro.data.AppEntry
import com.tahalabs.gamelauncherpro.data.BoostSettings
import com.tahalabs.gamelauncherpro.data.GameRepository
import com.tahalabs.gamelauncherpro.data.LibraryState
import com.tahalabs.gamelauncherpro.engine.Booster
import com.tahalabs.gamelauncherpro.engine.Perms
import com.tahalabs.gamelauncherpro.service.GameHudService
import kotlinx.coroutines.delay

data class Live(
    val mem: Booster.Mem,
    val batt: Booster.Battery,
    val storageFreeGb: Long,
    val session: String?
)

private fun readLive(ctx: Context): Live = Live(
    mem = Booster.memory(ctx),
    batt = Booster.battery(ctx),
    storageFreeGb = Booster.storageFreeGb(),
    session = if (GameHudService.running) (GameHudService.label ?: "Game") else null
)

@Composable
fun AppRoot(resumeTick: Int, visible: Boolean) {
    var splash by rememberSaveable { mutableStateOf(true) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Bg)
    ) {
        Crossfade(targetState = splash, animationSpec = tween(700), label = "root") { showSplash ->
            if (showSplash) {
                SplashScreen { splash = false }
            } else {
                MainShell(resumeTick = resumeTick, visible = visible)
            }
        }
    }
}

@Composable
fun MainShell(resumeTick: Int, visible: Boolean) {
    val ctx = LocalContext.current
    val repo = remember { GameRepository(ctx.applicationContext) }
    val lib = remember { LibraryState(repo) }
    val settings = remember {
        BoostSettings(ctx.getSharedPreferences("settings", Context.MODE_PRIVATE))
    }

    var tab by rememberSaveable { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var boosting by remember { mutableStateOf<AppEntry?>(null) }
    var live by remember { mutableStateOf(readLive(ctx)) }
    val perms = remember(resumeTick) { Perms.read(ctx) }

    LaunchedEffect(resumeTick) { lib.refresh() }
    LaunchedEffect(visible) {
        while (visible) {
            live = readLive(ctx)
            delay(2000)
        }
    }

    BackHandler(enabled = showAdd) { showAdd = false }
    BackHandler(enabled = !showAdd && boosting == null && tab != 0) { tab = 0 }

    val needsSetup = !(perms.overlay && perms.dnd && perms.notif)

    Box(
        Modifier
            .fillMaxSize()
            .background(Pal.Bg)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> GamesScreen(
                        lib = lib,
                        live = live,
                        needsSetup = needsSetup,
                        onLaunch = { boosting = it },
                        onAdd = { showAdd = true },
                        onSetup = { tab = 2 }
                    )
                    1 -> BoostScreen(
                        live = live,
                        settings = settings,
                        perms = perms,
                        onEndSession = {
                            GameHudService.stop(ctx)
                            live = live.copy(session = null)
                        }
                    )
                    else -> SetupScreen(perms = perms)
                }
            }
            BottomTabs(selected = tab, onSelect = { tab = it })
        }

        if (showAdd) {
            AddGamesScreen(lib = lib, onClose = { showAdd = false })
        }

        boosting?.let { game ->
            BoostSessionScreen(
                game = game,
                settings = settings,
                onCancel = { boosting = null },
                onDone = { boosting = null }
            )
        }
    }
}
