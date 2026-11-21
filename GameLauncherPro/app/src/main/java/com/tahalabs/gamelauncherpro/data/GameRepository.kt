package com.tahalabs.gamelauncherpro.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class AppEntry(
    val pkg: String,
    val label: String,
    val isGame: Boolean,
    val icon: ImageBitmap?
)

class GameRepository(private val ctx: Context) {

    private val pm = ctx.packageManager
    private val icons = ConcurrentHashMap<String, ImageBitmap>()
    private val prefs = ctx.getSharedPreferences("library", Context.MODE_PRIVATE)

    @Suppress("DEPRECATION")
    fun scan(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val seen = HashSet<String>()
        val out = ArrayList<AppEntry>()
        for (ri in pm.queryIntentActivities(intent, 0)) {
            val ai = ri.activityInfo.applicationInfo
            val pkg = ai.packageName
            if (pkg == ctx.packageName || !seen.add(pkg)) continue

            var icon = icons[pkg]
            if (icon == null) {
                icon = runCatching { ri.loadIcon(pm).toBitmap(128, 128).asImageBitmap() }.getOrNull()
                if (icon != null) icons[pkg] = icon
            }
            val isGame = ai.category == ApplicationInfo.CATEGORY_GAME ||
                (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            out.add(AppEntry(pkg, ri.loadLabel(pm).toString(), isGame, icon))
        }
        return out.sortedBy { it.label.lowercase() }
    }

    fun loadSet(name: String): Set<String> =
        prefs.getStringSet(name, emptySet())?.toSet() ?: emptySet()

    fun saveSet(name: String, value: Set<String>) {
        prefs.edit().putStringSet(name, value).apply()
    }
}

/**
 * Library = every installed app Android flags as a game (unless you removed it)
 * plus any app you added by hand.
 */
class LibraryState(private val repo: GameRepository) {

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set
    var loaded by mutableStateOf(false)
        private set
    var added by mutableStateOf(repo.loadSet("added"))
        private set
    var hidden by mutableStateOf(repo.loadSet("hidden"))
        private set

    val games: List<AppEntry>
        get() = apps.filter { inLibrary(it) }

    fun inLibrary(a: AppEntry): Boolean =
        a.pkg in added || (a.isGame && a.pkg !in hidden)

    suspend fun refresh() {
        val result = withContext(Dispatchers.Default) { repo.scan() }
        apps = result
        loaded = true
    }

    fun toggle(a: AppEntry) {
        if (inLibrary(a)) {
            added = added - a.pkg
            if (a.isGame) hidden = hidden + a.pkg
        } else {
            added = added + a.pkg
            hidden = hidden - a.pkg
        }
        repo.saveSet("added", added)
        repo.saveSet("hidden", hidden)
    }
}
