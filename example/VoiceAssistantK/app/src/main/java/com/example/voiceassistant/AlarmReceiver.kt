package com.example.voiceassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voiceassistant.NotificationService.Companion.LOOP_ACTION

class AlarmReceiver : BroadcastReceiver() {

    /**
     * trigger NotificationService
     */
    override fun onReceive(context: Context, intent: Intent) {
        context.sendBroadcast(Intent(LOOP_ACTION))
    }
}
