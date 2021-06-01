package com.example.hiemotion

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.net.Socket
import kotlin.concurrent.thread

class RecordReceiver(
    val context: Context,
    val updateCallback: (String)->Unit = {}
    ) : BroadcastReceiver() {
    val dataDir: String

    override fun onReceive(context: Context, intent: Intent) {
        var path: String? = null
        try {
            path = intent.extras!!.getString("path")
        } catch (e: Exception) {
            Log.d(TAG, "failed to extract path")
            e.printStackTrace()
        }

        if (path != null) {
            thread(true) {
                try {
                    val uri = "$dataDir/$path"
                    val iStream = BufferedInputStream(FileInputStream(uri))
                    val data = iStream.readBytes()

                    updateCallback(sendToServer(data))
                } catch (e: Exception) {
                    Log.d(TAG, "failed to get emotion")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun int2byte(i: Int): ByteArray {
        return byteArrayOf(
            (i and 0x000000FF).toByte(),
            (i shr 8).toByte(),
            (i shr 16).toByte(),
            (i shr 24).toByte()
        )
    }

    fun readFromStream(iStream: InputStream): String {
        val data = ArrayList<Byte>()

        var c = 0
        while (c != -1) {
            c = iStream.read()
            data.add(c.toByte())
        }

        data.removeLast()
        return String(data.toByteArray())
    }

    fun sendToServer(data: ByteArray): String {
        val socket = Socket(BuildConfig.SOCKET_HOST, BuildConfig.SOCKET_PORT)
        val oStream = socket.getOutputStream()
        val iStream = socket.getInputStream()

        oStream.write(int2byte(data.size))
        oStream.write(data)

        val emotion = readFromStream(iStream)

        iStream.close()
        oStream.close()
        socket.close()

        return emotion
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
