package com.example.miccentral

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import java.lang.Exception

class CentralService : Service() {
    private lateinit var dspRecorder: DSPRecorder

    private val requestReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                dspRecorder.setRecRequest(intent!!.extras!!.getLong(TIMESTAMP_KEY))
            } catch (e: Exception) {
                Log.d(TAG, "Failed on STTReceiver.onReceive()")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onCreate() {
        super.onCreate()

        val notificationUtil = NotificationUtil(this).apply {
            createNotificationChannel()
        }
        val notification = notificationUtil.createNotification()
        startForeground(NOTIFICATION_ID, notification)

        dspRecorder = DSPRecorder(this)
        dspRecorder.startRecording()

        registerReceiver(requestReceiver, IntentFilter(REC_REQUEST_ACTION))
    }


    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(requestReceiver)
        dspRecorder.stopRecording()
    }

    companion object {
        const val TAG = "CentralService"

        const val NOTIFICATION_ID = 7

        const val TIMESTAMP_KEY = "timestamp"
        const val REC_REQUEST_ACTION = "MIC_CENTRAL_REC_REQUEST"
    }
}
