package com.example.voiceassistant

import android.app.AlarmManager
import android.app.Notification.*
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.carpool.ds.Inform
import com.example.voiceassistant.MySpeechRecognizer.Companion.MIC_CENTRAL_REQUEST_ACTION

/**
 * NotificationListener
 * Search reply-able notifications, and alert activity
 */
class NotificationService : NotificationListenerService() {
    /***
     * LocalBinder
     * custom binder object that contains service object
     */
    class LocalBinder(val instance: NotificationService) : Binder()

    private val binder: IBinder = LocalBinder(this)
    private val regReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            registerActuation()
        }
    }
    private val triggerReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            triggerActuation()
        }
    }

    private val loopReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            setReplyNotification()
        }
    }

    private val initReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var path: String? = null
            var fromAudio: Boolean? = null

            try {
                path = intent!!.extras!!.getString("path")
                fromAudio = intent!!.extras!!.getBoolean("fromAudio")
            } catch (e: Exception) {
                Log.d(TAG, "failed to extract path")
                e.printStackTrace()
            }

            if (path != null && fromAudio != true) initAlarm()
        }
    }

    var replyNotification: StatusBarNotification? = null
    var replyNotificationExists: Boolean = false

    /***
     * onBind(intent)
     * This function returns self-defined LocalBinder object that contains service itself,
     * to allow activity call service functions.
     * Otherwise, it should return normal super()'s binder object for normal operation
     * related to system.
     */
    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action.equals(FAKE_BINDER_ACTION)) {
            Log.d(TAG, "Return custom binder object.")

            super.onBind(intent)
            return binder
        }
        Log.d(TAG, "Return original binder object.")
        return super.onBind(intent)
    }

    var initiated = false
    fun initAlarm() {
        if (!initiated) {
            initiated = true

            if (CPMode) onCreateCP()
            else onCreateNCP()
        }
    }

    override fun onCreate() {
        super.onCreate()

        registerReceiver(initReceiver, IntentFilter("MIC_CENTRAL_REC_RESPONSE"))
    }

    fun onCreateCP() {
        setAlarmCP(this, true)
        registerReceiver(regReceiver, IntentFilter(LOOP_ACTION))
        registerReceiver(triggerReceiver, IntentFilter(CH_NAME))
    }

    fun onCreateNCP() {
        setAlarmNCP(this)
        registerReceiver(loopReceiver, IntentFilter(LOOP_ACTION))
    }

    override fun onDestroy() {
        super.onDestroy()

        if (CPMode) onDestroyCP()
        else onDestroyNCP()

        unregisterReceiver(initReceiver)
    }

    fun onDestroyCP() {
        cancelAlarm()
        unregisterReceiver(regReceiver)
        unregisterReceiver(triggerReceiver)
    }

    fun onDestroyNCP() {
        cancelAlarm()
        unregisterReceiver(loopReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun registerActuation() {
        if (replyNotifications.isNullOrEmpty()) return

        val currTime = System.currentTimeMillis()
        val marginTime = 2 * 60 * 1000

        val sorted = replyNotifications.sortedWith(compareBy { it.notification.`when` })
        if ((currTime + marginTime) - sorted[0].notification.`when` >= notificationOlderThan) {
            replyNotification = sorted.first()
            replyNotificationExists = true

            val inform = Inform().apply {
                chName = CH_NAME
                actId = currTime
                category = "speak"
                start = currTime
                end = currTime + marginTime * 2
            }
            val intent = Intent("CARPOOL_ACTUATION").apply {
                putExtra("inform", inform)
            }
            sendBroadcast(intent)
        }
    }

    private fun triggerActuation() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(REPLY_SET_EXTRA_KEY, true)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun setReplyNotification() {
        if (replyNotifications.isNullOrEmpty()) return

        val currTime = System.currentTimeMillis()

        val sorted = replyNotifications.sortedWith(compareBy { it.notification.`when` })
        if (currTime - sorted[0].notification.`when` >= notificationOlderThan) {
            replyNotification = sorted.first()
            replyNotificationExists = true

            triggerActuation()
        }
    }

    fun unsetReplyNotification() {
        if (replyNotificationExists) {
            cancelNotification(replyNotification!!.key)

            replyNotificationExists = false
            replyNotification = null
        }
    }

    /***
     * logNotifications(), logAllNotifications()
     * Log notifications for debugging.
     */
    fun logNotifications(notifications: Array<StatusBarNotification>) {
        for ((idx, it) in notifications.withIndex()) {
            val packageName = it.packageName
            val postTime = it.postTime

            val category = it.notification.category
            val extras = it.notification.extras
            val actions = it.notification.actions

            val title = extras.getCharSequence(EXTRA_TITLE)
            val text = extras.getCharSequence(EXTRA_TEXT)
            val bigText = extras.getCharSequence(EXTRA_BIG_TEXT)
            val subText = extras.getCharSequence(EXTRA_SUB_TEXT)

            val actionTitles: String = if (actions.isNullOrEmpty()) ""
            else actions.joinToString(", ", "[", "]") { it.title }

            Log.d(
                TAG,
                "[%2d] Notification from: %s at %s".format(idx, packageName, postTime.toString())
            )
            Log.d(TAG, "\t\tTitle: %s, Text: %s, SubText: %s".format(title, text, subText))
            Log.d(TAG, "\t\tBigText: %s, Actions: %s".format(bigText, actionTitles))
            Log.d(TAG, "\t\tCategory is: %s".format(category))
        }
    }

    fun logAllNotifications() {
        logNotifications(activeNotifications)
    }

    /***
     * replyNotifications
     * return notifications which is reply-able
     */
    private val replyNotifications: Array<StatusBarNotification>
        get() {
            return activeNotifications.filter { !it.notification.actions.isNullOrEmpty() }
                .filter {
                    it.notification.actions.map { act -> act.title }
                        .any { ti -> REPLY_WORDS.contains(ti) }
                }
                .filter { REPLY_CATEGORY.contains(it.notification.category) }
                .toTypedArray()
        }

    companion object {
        const val TAG = "VA.NotificationListener"
        val CPMode = true

        const val FAKE_BINDER_ACTION = "com.example.voice_assistant.ACTION_FAKE_BINDER"
        const val LOOP_ACTION = "com.example.voice_assistant.BR_INTENT"

        const val REPLY_SET_EXTRA_KEY = "com.example.voice_assistant.EXTRA"

        val REPLY_WORDS = arrayOf("reply", "??????")
        val REPLY_CATEGORY = arrayOf(CATEGORY_EMAIL, CATEGORY_MESSAGE)

        const val CH_NAME = "com.example.voice_assistant.CH"

        private lateinit var alarmManager: AlarmManager
        private lateinit var pendingIntent: PendingIntent

        const val alertPeriod = 3 * 60 * 1000
        const val notificationOlderThan = 1 * 60 * 1000

        fun setAlarmCP(context: Context, init: Boolean = false) {
            var triggerAt = alertPeriod
            if (init) triggerAt = 1 * 60 * 1000

            val intent = Intent(context, AlarmReceiver::class.java)
            pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, 0)

            alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + triggerAt,
                pendingIntent
            )
        }

        fun setAlarmNCP(context: Context) {
            val intent = Intent(context, AlarmReceiver::class.java)
            pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, 0)

            alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + alertPeriod,
                pendingIntent
            )
        }

        private fun cancelAlarm() {
            alarmManager.cancel(pendingIntent)
        }
    }
}
