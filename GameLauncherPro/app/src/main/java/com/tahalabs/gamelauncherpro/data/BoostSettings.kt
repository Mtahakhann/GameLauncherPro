package com.tahalabs.gamelauncherpro.data

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf

/** Which things a boost session does. Persisted, and observable from Compose. */
class BoostSettings(private val prefs: SharedPreferences) {

    companion object {
        const val CLEAR_RAM = "clear_ram"
        const val DND = "dnd"
        const val LIFT_LIMITS = "lift_limits"
        const val ANIMATIONS = "animations"
        const val HUD = "hud"
        const val GUARD = "guard"

        private val DEFAULTS = mapOf(
            CLEAR_RAM to true,
            DND to true,
            LIFT_LIMITS to true,
            ANIMATIONS to false,
            HUD to true,
            GUARD to true
        )
    }

    private val state = mutableStateMapOf<String, Boolean>().apply {
        DEFAULTS.forEach { (k, d) -> put(k, prefs.getBoolean(k, d)) }
    }

    operator fun get(key: String): Boolean = state[key] ?: false

    operator fun set(key: String, value: Boolean) {
        state[key] = value
        prefs.edit().putBoolean(key, value).apply()
    }
}
