package com.tahalabs.gamelauncherpro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tahalabs.gamelauncherpro.data.AppEntry
import com.tahalabs.gamelauncherpro.data.LibraryState
import com.tahalabs.gamelauncherpro.engine.Booster

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GamesScreen(
    lib: LibraryState,
    live: Live,
    needsSetup: Boolean,
    onLaunch: (AppEntry) -> Unit,
    onAdd: () -> Unit,
    onSetup: () -> Unit
) {
    var removing by remember { mutableStateOf<AppEntry?>(null) }
    val games = lib.games

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Your games",
                                color = Pal.Text,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Light
                            )
                            Spacer(Modifier.height(2.dp))
                            Text("Game Launcher Pro", color = Pal.Accent, fontSize = 13.sp)
                        }
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .border(1.dp, Pal.Line, CircleShape)
                                .clickable(onClick = onAdd),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Pal.Accent, fontSize = 26.sp, fontWeight = FontWeight.Light)
                        }
                    }
                    Spacer(Modifier.height(20.dp))

                    Panel(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Memory", color = Pal.Dim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(
                                "${Booster.gb(live.mem.usedMb)} of ${Booster.gb(live.mem.totalMb)} GB in use",
                                color = Pal.Text,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        LinearMeter(live.mem.usedFraction, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Tap a game to boost and launch it. Hold to remove it.",
                            color = Pal.Faint,
                            fontSize = 12.sp
                        )
                    }

                    if (needsSetup) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, Pal.Warn.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                                .clickable(onClick = onSetup)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Finish setup to unlock the full boost",
                                color = Pal.Warn,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text("Open", color = Pal.Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    SectionLabel("Library (${games.size})")
                }
            }

            if (lib.loaded && games.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(vertical = 28.dp)) {
                        Text("No games yet", color = Pal.Text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Games you install appear here automatically. Tap + to add any app by hand.",
                            color = Pal.Dim,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            items(games, key = { it.pkg }) { app ->
                GameTile(app = app, onClick = { onLaunch(app) }, onLongClick = { removing = app })
            }
        }

        removing?.let { app ->
            AlertDialog(
                onDismissRequest = { removing = null },
                containerColor = Pal.Raised,
                title = { Text("Remove ${app.label}?", color = Pal.Text, fontSize = 18.sp) },
                text = {
                    Text(
                        "It leaves your library. The game stays installed.",
                        color = Pal.Dim,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { lib.toggle(app); removing = null }) {
                        Text("Remove", color = Pal.Hot)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { removing = null }) {
                        Text("Cancel", color = Pal.Dim)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GameTile(app: AppEntry, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .clip(shape)
            .background(Pal.Surface)
            .border(1.dp, Pal.Line, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = app.icon
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = app.label,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
        } else {
            Box(
                Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Pal.Raised),
                contentAlignment = Alignment.Center
            ) {
                Text(app.label.take(1), color = Pal.Accent, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            app.label,
            color = Pal.Text,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
