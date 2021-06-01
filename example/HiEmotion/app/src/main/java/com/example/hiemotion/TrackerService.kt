package com.example.hiemotion

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.*

class TrackerService : Service() {
    lateinit var notificationUtil: NotificationUtil
    lateinit var recordReceiver: RecordReceiver
    lateinit var notification: Notification

    override fun onCreate() {
        super.onCreate()

        notificationUtil = NotificationUtil(this).apply {
            createNotificationChannel()
        }
        notification = notificationUtil.createNotification()
        startForeground(NOTIFICATION_ID, notification)

        recordReceiver = RecordReceiver(this) {
            notification = notificationUtil.createNotification(
                "Last detect: ${Calendar.getInstance().time}.\nEmotion: ${it}."
            )
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
        registerReceiver(recordReceiver, IntentFilter("MIC_CENTRAL_REC_RESPONSE"))
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(recordReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    companion object {
        const val NOTIFICATION_ID = 10
    }
}
