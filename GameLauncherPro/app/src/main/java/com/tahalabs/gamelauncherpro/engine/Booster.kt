package com.tahalabs.gamelauncherpro.engine

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Everything that actually touches the system lives here.
 * Only uses public Android APIs (no root). Anything that needs a special
 * permission degrades gracefully and reports back instead of crashing.
 */
object Booster {

    private const val MB = 1024L * 1024L
    private const val LOW_POWER = "low_power"
    private val ANIM_KEYS = listOf(
        Settings.Global.WINDOW_ANIMATION_SCALE,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        Settings.Global.ANIMATOR_DURATION_SCALE
    )

    // ---------- live stats ----------

    data class Mem(val totalMb: Long, val availMb: Long) {
        val usedMb: Long get() = totalMb - availMb
        val usedFraction: Float
            get() = if (totalMb <= 0L) 0f else (usedMb.toFloat() / totalMb).coerceIn(0f, 1f)
    }

    data class Battery(val level: Int, val tempC: Float, val charging: Boolean)

    data class TrimResult(val swept: Int, val freedMb: Long)

    fun memory(ctx: Context): Mem {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return Mem(info.totalMem / MB, info.availMem / MB)
    }

    fun battery(ctx: Context): Battery {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val temp = i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val status = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return Battery(pct, temp / 10f, charging)
    }

    fun storageFreeGb(): Long {
        return try {
            StatFs(Environment.getDataDirectory().path).availableBytes / (1024L * MB)
        } catch (_: Throwable) {
            0L
        }
    }

    fun gb(mb: Long): String = String.format(Locale.US, "%.1f", mb / 1024f)

    // ---------- RAM ----------

    /** Signals every launchable user app (except protected ones) to drop its background processes. */
    @Suppress("DEPRECATION")
    fun killBackground(ctx: Context, keep: Set<String>): Int {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val pm = ctx.packageManager

        val protectedPkgs = HashSet<String>(keep)
        protectedPkgs.add(ctx.packageName)
        protectedPkgs.add("com.android.systemui")
        runCatching {
            pm.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0
            )?.activityInfo?.packageName
        }.getOrNull()?.let { protectedPkgs.add(it) }
        runCatching {
            ctx.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        }.getOrNull()?.let { protectedPkgs.add(it) }
        runCatching {
            ctx.getSystemService(InputMethodManager::class.java)
                ?.enabledInputMethodList?.forEach { protectedPkgs.add(it.packageName) }
        }

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val seen = HashSet<String>()
        var swept = 0
        for (ri in pm.queryIntentActivities(launcherIntent, 0)) {
            val p = ri.activityInfo.packageName
            if (p in protectedPkgs || !seen.add(p)) continue
            try {
                am.killBackgroundProcesses(p)
                swept++
            } catch (_: Throwable) {
            }
        }
        return swept
    }

    suspend fun clearRam(ctx: Context, keep: Set<String>): TrimResult =
        withContext(Dispatchers.Default) {
            val before = memory(ctx)
            val swept = killBackground(ctx, keep)
            delay(900)
            val after = memory(ctx)
            TrimResult(swept, (after.availMb - before.availMb).coerceAtLeast(0L))
        }

    // ---------- session settings (saved so they can always be restored) ----------

    private fun sp(ctx: Context) =
        ctx.getSharedPreferences("session", Context.MODE_PRIVATE)

    fun hasSecureAccess(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** Priority-only Do Not Disturb. Returns false if access has not been granted. */
    fun enableDnd(ctx: Context): Boolean {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (!nm.isNotificationPolicyAccessGranted) return false
        val s = sp(ctx)
        if (!s.contains("dnd_prev")) {
            s.edit().putInt("dnd_prev", nm.currentInterruptionFilter).apply()
        }
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        return true
    }

    /** true = was on and is now off, false = already off, null = not possible. */
    fun disableBatterySaver(ctx: Context): Boolean? {
        if (!hasSecureAccess(ctx)) return null
        return try {
            val cr = ctx.contentResolver
            val on = Settings.Global.getInt(cr, LOW_POWER, 0) == 1
            if (on) {
                val s = sp(ctx)
                if (!s.contains("lp_prev")) s.edit().putInt("lp_prev", 1).apply()
                Settings.Global.putInt(cr, LOW_POWER, 0)
            }
            on
        } catch (_: Throwable) {
            null
        }
    }

    fun speedUpAnimations(ctx: Context): Boolean {
        if (!hasSecureAccess(ctx)) return false
        return try {
            val cr = ctx.contentResolver
            val s = sp(ctx)
            val e = s.edit()
            for (k in ANIM_KEYS) {
                if (!s.contains("anim_$k")) {
                    e.putFloat("anim_$k", Settings.Global.getFloat(cr, k, 1f))
                }
                Settings.Global.putFloat(cr, k, 0.5f)
            }
            e.apply()
            true
        } catch (_: Throwable) {
            false
        }
    }

    /** Puts DND, battery saver and animation speed back exactly how they were. Safe to call anytime. */
    fun restoreSession(ctx: Context) {
        val s = sp(ctx)
        if (s.all.isEmpty()) return
        runCatching {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (s.contains("dnd_prev") && nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(
                    s.getInt("dnd_prev", NotificationManager.INTERRUPTION_FILTER_ALL)
                )
            }
        }
        runCatching {
            if (s.contains("lp_prev") && hasSecureAccess(ctx)) {
                Settings.Global.putInt(ctx.contentResolver, LOW_POWER, s.getInt("lp_prev", 0))
            }
        }
        runCatching {
            if (hasSecureAccess(ctx)) {
                for (k in ANIM_KEYS) {
                    if (s.contains("anim_$k")) {
                        Settings.Global.putFloat(ctx.contentResolver, k, s.getFloat("anim_$k", 1f))
                    }
                }
            }
        }
        s.edit().clear().apply()
    }
}
