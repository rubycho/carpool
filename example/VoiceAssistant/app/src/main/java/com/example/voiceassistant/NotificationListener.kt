package com.example.voiceassistant

import android.app.Notification.*
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.voiceassistant.util.NotificationProxy


class NotificationListener : NotificationListenerService() {
    /***
     * LocalBinder
     * custom binder object that contains service object
     */
    class LocalBinder(val instance: NotificationListener) : Binder()

    private val mBinder: IBinder = LocalBinder(this)
    private val notificationProxy = NotificationProxy(this)

    override fun onCreate() {
        super.onCreate()
        notificationProxy.createNotificationChannel()
    }

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
            return mBinder
        }
        Log.d(TAG, "Return original binder object.")
        return super.onBind(intent)
    }

    /***
     * logNotifications(), readAllNotifications()
     * Log notifications for debugging.
     */
    fun logNotifications(notifications: Collection<StatusBarNotification>) {
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

            Log.d(TAG, "[%2d] Notification from: %s at %s".format(idx, packageName, postTime.toString()))
            Log.d(TAG, "\t\tTitle: %s, Text: %s, SubText: %s".format(title, text, subText))
            Log.d(TAG, "\t\tBigText: %s, Actions: %s".format(bigText, actionTitles))
            Log.d(TAG, "\t\tCategory is: %s".format(category))
        }
    }
    fun readAllNotifications() { logNotifications(activeNotifications.toList()) }

    /***
     * replyNotifications()
     * return notifications which is reply-able
     */
    fun replyNotifications(): Collection<StatusBarNotification> {
        return activeNotifications.filter { !it.notification.actions.isNullOrEmpty() }
            .filter { it.notification.actions.map { act -> act.title }.any { ti -> REPLY_WORDS.contains(ti) }}
            .filter { REPLY_CATEGORY.contains(it.notification.category) }
    }

    companion object {
        const val TAG = "VA.NotificationListener"
        const val FAKE_BINDER_ACTION = "com.example.voice_assistant.ACTION_FAKE_BINDER"

        val REPLY_WORDS = arrayOf("reply", "답장")
        val REPLY_CATEGORY = arrayOf(CATEGORY_EMAIL, CATEGORY_MESSAGE)
    }
}
