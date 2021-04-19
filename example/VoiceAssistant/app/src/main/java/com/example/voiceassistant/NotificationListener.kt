package com.example.voiceassistant

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.util.Log


class NotificationListener : NotificationListenerService() {
    companion object {
        const val TAG = "com.example.voice_assistant.NotificationListener"
        const val FAKE_BINDER_ACTION = "com.example.voice_assistant.ACTION_FAKE_BINDER"
    }

    /* custom binder object that contains service object */
    class LocalBinder(val instance: NotificationListener) : Binder()
    private val mBinder: IBinder = LocalBinder(this)

    override fun onBind(intent: Intent?): IBinder? {
        /* should return local binder only when bind() is called on activity!! */
        if (intent?.action.equals(FAKE_BINDER_ACTION)) {
            Log.d(TAG, "Return custom binder object.")

            super.onBind(intent)
            return mBinder
        }
        Log.d(TAG, "Return original binder object.")
        return super.onBind(intent)
    }
}
