package io.github.mickaelmagniez.windbubble.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import io.github.mickaelmagniez.windbubble.MainActivity
import io.github.mickaelmagniez.windbubble.R

internal object OverlayNotifications {

    const val CHANNEL_ID = "wind_overlay"
    const val NOTIFICATION_ID = 42

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.overlay_notification_channel),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.overlay_notification_channel_description)
            setShowBadge(false)
        }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    fun build(context: Context): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            context,
            1,
            Intent(context, WindOverlayService::class.java).setAction(WindOverlayService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(context.getString(R.string.overlay_notification_title))
            .setContentText(context.getString(R.string.overlay_notification_text))
            .setContentIntent(contentIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.overlay_notification_stop),
                stopIntent,
            )
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
