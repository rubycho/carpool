package com.example.voiceassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voiceassistant.NotificationService.Companion.BR_ACTION

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.sendBroadcast(Intent(BR_ACTION))
    }
}
