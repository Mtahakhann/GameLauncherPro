package com.tahalabs.gamelauncherpro

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tahalabs.gamelauncherpro.engine.Booster
import com.tahalabs.gamelauncherpro.service.GameHudService
import com.tahalabs.gamelauncherpro.ui.AppRoot
import com.tahalabs.gamelauncherpro.ui.LauncherTheme

class MainActivity : ComponentActivity() {

    private var resumeTick by mutableIntStateOf(0)
    private var visible by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        // If the app process died mid-session, put the phone's settings back.
        if (!GameHudService.running) Booster.restoreSession(applicationContext)

        setContent {
            LauncherTheme {
                AppRoot(resumeTick = resumeTick, visible = visible)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        visible = true
        resumeTick++
    }

    override fun onPause() {
        super.onPause()
        visible = false
    }
}
