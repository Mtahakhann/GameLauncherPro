package com.tahalabs.gamelauncherpro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.tahalabs.gamelauncherpro.MainActivity
import com.tahalabs.gamelauncherpro.R
import com.tahalabs.gamelauncherpro.engine.Booster
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Owns the "game session": shows the live HUD, runs Live Guard, and
 * restores DND / battery saver / animations the moment the session ends.
 */
class GameHudService : Service() {

    companion object {
        const val ACTION_STOP = "com.tahalabs.gamelauncherpro.STOP"
        private const val EXTRA_PKG = "pkg"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_HUD = "hud"
        private const val EXTRA_GUARD = "guard"
        private const val CHANNEL = "session"
        private const val NOTIF_ID = 7

        @Volatile var running = false
        @Volatile var label: String? = null

        fun start(ctx: Context, pkg: String, label: String, hud: Boolean, guard: Boolean) {
            val i = Intent(ctx, GameHudService::class.java)
                .putExtra(EXTRA_PKG, pkg)
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_HUD, hud)
                .putExtra(EXTRA_GUARD, guard)
            ctx.startForegroundService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, GameHudService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private var wm: WindowManager? = null
    private var hudView: TextView? = null
    private var pkg: String? = null
    private var guard = false
    private var lastGuard = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            Booster.restoreSession(applicationContext)
            stopSelf()
            return START_NOT_STICKY
        }

        pkg = intent?.getStringExtra(EXTRA_PKG)
        label = intent?.getStringExtra(EXTRA_LABEL)
        guard = intent?.getBooleanExtra(EXTRA_GUARD, false) ?: false
        val wantHud = intent?.getBooleanExtra(EXTRA_HUD, false) ?: false

        createChannel()
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
        running = true

        if (wantHud && Settings.canDrawOverlays(this)) showHud() else removeHud()

        lastGuard = SystemClock.elapsedRealtime()
        handler.removeCallbacks(tick)
        handler.post(tick)
        return START_NOT_STICKY
    }

    private val tick = object : Runnable {
        override fun run() {
            updateHud()
            val now = SystemClock.elapsedRealtime()
            if (guard && now - lastGuard >= 60_000L) {
                lastGuard = now
                val keep = setOfNotNull(pkg)
                io.execute { Booster.killBackground(applicationContext, keep) }
            }
            handler.postDelayed(this, 2000L)
        }
    }

    // ---------- HUD ----------

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun showHud() {
        if (hudView != null) return
        val manager = getSystemService(WindowManager::class.java)
        wm = manager
        val tv = TextView(this).apply {
            setTextColor(Color.parseColor("#F4F4F6"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#B30A0B0F"))
                setStroke(dp(1), Color.parseColor("#553DDC97"))
            }
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(6)
        }
        try {
            manager.addView(tv, lp)
            hudView = tv
        } catch (_: Throwable) {
            hudView = null
        }
    }

    private fun removeHud() {
        hudView?.let { v -> runCatching { wm?.removeView(v) } }
        hudView = null
    }

    private fun updateHud() {
        val tv = hudView ?: return
        val m = Booster.memory(this)
        val b = Booster.battery(this)
        val status = getSystemService(PowerManager::class.java).currentThermalStatus
        val state = when {
            status <= 0 -> "COOL"
            status == 1 -> "WARM"
            status == 2 -> "HOT"
            else -> "THROTTLING"
        }
        tv.setTextColor(
            Color.parseColor(
                when {
                    status >= 3 -> "#FF5D6C"
                    status == 2 -> "#FFB454"
                    else -> "#F4F4F6"
                }
            )
        )
        tv.text = String.format(
            Locale.US, "RAM %sG free  \u2022  %.0f\u00B0C  \u2022  %d%%  \u2022  %s",
            Booster.gb(m.availMb), b.tempC, b.level, state
        )
    }

    // ---------- notification ----------

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Game session", NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, GameHudService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val action = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_stat_bolt), "End session", stop
        ).build()
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_bolt)
            .setContentTitle("Game session active")
            .setContentText(label?.let { "Boosted for $it" } ?: "Performance mode is on")
            .setContentIntent(open)
            .addAction(action)
            .setOngoing(true)
            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeHud()
        io.shutdown()
        running = false
        label = null
        Booster.restoreSession(applicationContext)
        super.onDestroy()
    }
}
