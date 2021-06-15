package com.example.carpool

import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import com.example.carpool.ds.Inform
import com.example.carpool.ds.Request
import com.example.carpool.ds.RequestDismiss
import com.example.carpool.ds.Trigger
import java.util.*

class CARPoolManager : Service() {

    val requestQueue: Queue<Request> = LinkedList()
    val informQueue: Queue<Inform> = LinkedList()

    lateinit var sensingReceiver: SensingReceiver
    lateinit var actuateReceiver: ActuateReceiver

    lateinit var timer: Timer
    lateinit var timerTask: TimerTask

    lateinit var notificationUtil: NotificationUtil
    lateinit var notification: Notification

    class SensingReceiver(
        val cb1: (Request) -> Unit,
        val cb2: (RequestDismiss) -> Unit
    ) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val request = intent?.getParcelableExtra<Request>("request")
            val dismiss = intent?.getParcelableExtra<RequestDismiss>("dismiss")

            if (request != null) cb1(request)
            else if (dismiss != null) cb2(dismiss)
            else return
        }
    }

    class ActuateReceiver(val cb: (Inform) -> Unit) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val inform = intent?.getParcelableExtra<Inform>("inform") ?: return
            cb(inform)
        }
    }

    fun onInform(inform: Inform) {
        synchronized(informQueue) {
            Log.d(TAG, "Received inform: category: ${inform.category}, end: ${inform.end}")
            informQueue.add(inform)
        }
    }

    fun onRequest(request: Request) {
        synchronized(requestQueue) {
            Log.d(TAG, "Received request: reqId: ${request.reqId}, category: ${request.category}")
            requestQueue.add(request)
        }
    }

    fun onDismiss(dismiss: RequestDismiss) {
        synchronized(requestQueue) {
            Log.d(TAG, "Received dismiss: reqId: ${dismiss.reqId}")

            val item =
                requestQueue.find { it.chName == dismiss.chName && it.reqId == dismiss.reqId }
            if (item != null)
                requestQueue.remove(item)
        }
    }

    fun triggerActuation(inform: Inform) {
        val trigger = Trigger().apply {
            actId = inform.actId
            invokeAt = "NOW"
        }
        val intent = Intent(inform.chName).apply {
            putExtra("trigger", trigger)
        }
        sendBroadcast(intent)
    }

    fun doMatching() {
        Log.d(TAG, "running!")

        val imminentTime = 60 * 1000

        // group informQueue and requestQueue
        // for cat if (cat in inf) && (req in inf) => trigger
        synchronized(informQueue) {
            synchronized(requestQueue) {
                Log.d(TAG, "CurrTime: ${System.currentTimeMillis()}")

                Log.d(TAG, "informQueue")
                for (inform in informQueue)
                    Log.d(TAG, "${inform.category}, ${inform.end}")

                Log.d(TAG, "reqQueue")
                for (req in requestQueue)
                    Log.d(TAG, req.category)

                val informGrp = informQueue.groupBy { it.category }
                val reqGrp = requestQueue.groupBy { it.category }

                val informGrpKeys = informGrp.keys
                val reqGrpKeys = reqGrp.keys

                for (key in informGrpKeys) {
                    if (key in reqGrpKeys) {
                        val item = informGrp[key]!!.sortedWith(compareBy { it.end })[0]
                        triggerActuation(item)
                        informQueue.remove(item)
                    }
                }
            }
        }

        // trigger imminent actuation
        synchronized(informQueue) {
            val cpyQueue = informQueue.toList()
            for (inform in cpyQueue) {
                if ((inform.end - System.currentTimeMillis() < imminentTime)) {
                    triggerActuation(inform)
                    informQueue.remove(inform)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        notificationUtil = NotificationUtil(this).apply {
            createNotificationChannel()
        }
        notification = notificationUtil.createNotification()
        startForeground(NOTIFICATION_ID, notification)

        sensingReceiver = SensingReceiver(this::onRequest, this::onDismiss)
        actuateReceiver = ActuateReceiver(this::onInform)

        registerReceiver(sensingReceiver, IntentFilter(SENSING_ACTION))
        registerReceiver(actuateReceiver, IntentFilter(ACTUATE_ACTION))

        timer = Timer()
        timerTask = object: TimerTask() {
            override fun run() {
                doMatching()
            }
        }

        val matchingPeriod = 3 * 1000L
        timer.scheduleAtFixedRate(
            timerTask, 0, matchingPeriod
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(sensingReceiver)
        unregisterReceiver(actuateReceiver)

        timer.cancel()
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    companion object {
        const val TAG = "CARPoolManager"

        const val SENSING_ACTION = "CARPOOL_SENSING"
        const val ACTUATE_ACTION = "CARPOOL_ACTUATION"

        const val NOTIFICATION_ID = 11
    }
}
