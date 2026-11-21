package com.tahalabs.gamelauncherpro.engine

import android.app.NotificationManager
import android.content.Context
import android.provider.Settings

data class Perms(
    val overlay: Boolean,
    val dnd: Boolean,
    val notif: Boolean,
    val secure: Boolean
) {
    companion object {
        fun read(ctx: Context): Perms {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            return Perms(
                overlay = Settings.canDrawOverlays(ctx),
                dnd = nm.isNotificationPolicyAccessGranted,
                notif = nm.areNotificationsEnabled(),
                secure = Booster.hasSecureAccess(ctx)
            )
        }
    }
}
