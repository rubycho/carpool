package com.example.voiceassistant.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.voiceassistant.BuildConfig
import com.example.voiceassistant.R

class NotificationProxy (private val context: Context) {
    private var builder: NotificationCompat.Builder? = null

    fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun pushNotification(contentText: String) {
        if (builder == null) {
            builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Notification List")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(true)
        }
        builder!!.setContentText(contentText)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder!!.build())
        }
    }

    companion object {
        const val CHANNEL_NAME = "Default Channel"
        const val CHANNEL_DESCRIPTION = "Default channel for pushing notifications."
        const val CHANNEL_ID = BuildConfig.APPLICATION_ID

        const val NOTIFICATION_ID = 7
    }
}
