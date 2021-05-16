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
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            setReplyableNotification()
        }
    }

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent

    var replyableNotification: StatusBarNotification? = null
    var replyableNotificationExists: Boolean = false

    private val debug = false

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

    override fun onCreate() {
        super.onCreate()

        setAlarm()
        registerReceiver(receiver, IntentFilter(BR_ACTION))
    }

    override fun onDestroy() {
        super.onDestroy()

        cancelAlarm()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun setReplyableNotification() {
        if (replyNotifications.isNullOrEmpty()) return

        replyableNotification = replyNotifications.first()
        replyableNotificationExists = true

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(INTENT_EXTRA_KEY, true)

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    fun unsetReplyableNotification() {
        if (replyableNotificationExists) {
            replyableNotificationExists = false
            replyableNotification = null
        }
    }

    private fun setAlarm() {
        val intent = Intent(applicationContext, AlarmReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, intent, 0)

        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        if (!debug)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                AlarmManager.INTERVAL_FIFTEEN_MINUTES,
                pendingIntent
            )
        /* debug purpose */
        else
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 15 * 1000,
                pendingIntent
            )
    }

    private fun cancelAlarm() {
        alarmManager.cancel(pendingIntent)
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
        const val FAKE_BINDER_ACTION = "com.example.voice_assistant.ACTION_FAKE_BINDER"
        const val BR_ACTION = "com.example.voice_assistant.BR_INTENT"
        const val INTENT_EXTRA_KEY = "com.example.voice_assistant.EXTRA"

        val REPLY_WORDS = arrayOf("reply", "답장")
        val REPLY_CATEGORY = arrayOf(CATEGORY_EMAIL, CATEGORY_MESSAGE)
    }
}
