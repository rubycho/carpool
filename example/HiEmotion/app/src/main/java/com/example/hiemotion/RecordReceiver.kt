package com.example.hiemotion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import java.io.BufferedInputStream
import java.io.FileInputStream
import kotlin.concurrent.thread

class RecordReceiver(
    val context: Context,
    val updateCallback: (String)->Unit = {}
    ) : BroadcastReceiver() {
    val dataDir: String

    override fun onReceive(context: Context, intent: Intent) {
        var path: String? = null
        var fromAudio: Boolean? = null
        try {
            path = intent.extras!!.getString("path")
            fromAudio = intent.extras!!.getBoolean("fromAudio")
        } catch (e: Exception) {
            Log.d(TAG, "failed to extract path")
            e.printStackTrace()
        }

        if (fromAudio == null) return
        if (fromAudio) return

        if (path != null) {
            thread(true) {
                try {
                    val uri = "$dataDir/$path"
                    val iStream = BufferedInputStream(FileInputStream(uri))
                    val data = iStream.readBytes()

                    val emotion = SocketClient.sendWav(data)
                    updateCallback(emotion)
                } catch (e: Exception) {
                    Log.d(TAG, "failed to get emotion")
                    e.printStackTrace()
                }
            }
        }
    }

    init {
        val packageName = "com.example.miccentral"
        val pkgManager = context.packageManager
        val appInfo = pkgManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        dataDir = "${appInfo.dataDir}/files"
    }

    companion object {
        const val TAG = "RecordReceiver"
    }
}
