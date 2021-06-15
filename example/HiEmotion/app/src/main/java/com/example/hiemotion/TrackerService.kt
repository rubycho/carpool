package com.example.hiemotion

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import com.example.carpool.ds.Request
import com.example.carpool.ds.RequestDismiss
import java.util.*

class TrackerService : Service() {
    lateinit var notificationUtil: NotificationUtil
    lateinit var recordReceiver: RecordReceiver
    lateinit var notification: Notification

    var timer: Timer? = null
    lateinit var timerTask: TimerTask

    var emotionConfidence = 0F
    var sentSpeakRequest = false

    var _reqId = 0L

    val emotionDurationSec = 30L
    val degradeProb = 0.8F
    val vagueProb = 0.5F

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

            timer?.cancel()
            synchronized(emotionConfidence) {
                emotionConfidence = 1F
                SocketClient.sendConfidence(emotionConfidence)

                Log.d(TAG, "confidence: %f".format(emotionConfidence))
                // cancel platform
                if (sentSpeakRequest) {
                    val dismiss = RequestDismiss().apply {
                        chName = CH_NAME
                        reqId = _reqId
                    }
                    val intent = Intent("CARPOOL_SENSING").apply {
                        putExtra("dismiss", dismiss)
                    }
                    sendBroadcast(intent)
                }
                sentSpeakRequest = false

                timer = Timer()
                timerTask = object: TimerTask() {
                    override fun run() = degradeConfidence()
                }
                timer?.scheduleAtFixedRate(timerTask, emotionDurationSec * 1000, emotionDurationSec * 1000)
            }
        }
        registerReceiver(recordReceiver, IntentFilter("MIC_CENTRAL_REC_RESPONSE"))
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(recordReceiver)
        timer?.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    fun degradeConfidence() {
        synchronized(emotionConfidence) {
            emotionConfidence *= degradeProb
            SocketClient.sendConfidence(emotionConfidence)
            Log.d(TAG, "confidence: %f".format(emotionConfidence))

            if (!sentSpeakRequest && emotionConfidence < vagueProb) {
                // send to platform
                _reqId = System.currentTimeMillis()
                val request = Request().apply {
                    chName = CH_NAME
                    reqId = _reqId
                    category = "speak"
                }
                val intent = Intent("CARPOOL_SENSING").apply {
                    putExtra("request", request)
                }
                sendBroadcast(intent)

                Log.d(TAG, "Platform SOS!")
                sentSpeakRequest = true
            }
        }
    }

    companion object {
        const val TAG = "HE.TrackerService"

        const val CH_NAME = "com.example.hiemotion.CH"

        const val NOTIFICATION_ID = 10
    }
}
