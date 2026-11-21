package com.tahalabs.gamelauncherpro.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tahalabs.gamelauncherpro.data.AppEntry
import com.tahalabs.gamelauncherpro.data.LibraryState

@Composable
fun AddGamesScreen(lib: LibraryState, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    BackHandler(onBack = onClose)

    val list = remember(lib.apps, query) {
        lib.apps.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Pal.Bg)
            .pointerInput(Unit) { detectTapGestures { } }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Add games", color = Pal.Text, fontSize = 28.sp, fontWeight = FontWeight.Light)
                Text(
                    "Tick anything you want in your library.",
                    color = Pal.Dim,
                    fontSize = 13.sp
                )
            }
            PillButton("Done", compact = true, onClick = onClose)
        }

        SearchField(query) { query = it }
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
        ) {
            items(list, key = { it.pkg }) { app ->
                AppRow(app = app, selected = lib.inLibrary(app)) { lib.toggle(app) }
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Pal.Surface)
            .border(1.dp, Pal.Line, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (value.isEmpty()) {
            Text("Search apps", color = Pal.Faint, fontSize = 14.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Pal.Text, fontSize = 14.sp),
            cursorBrush = SolidColor(Pal.Accent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppRow(app: AppEntry, selected: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = app.icon
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Pal.Raised)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                color = Pal.Text,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (app.isGame) {
                Text("Detected as a game", color = Pal.Dim, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.background(Pal.Accent)
                    else Modifier.border(1.5.dp, Pal.Line, CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Text("\u2713", color = Pal.Bg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
