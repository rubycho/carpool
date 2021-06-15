package com.example.voiceassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlin.concurrent.thread

class MySpeechRecognizer(
    private val context: Context,
    private val callback: Callback
) : BroadcastReceiver() {

    interface Callback {
        fun onSpeechEnd()
        fun onResult(s: String)
        fun onStop()
        fun onFinal()
    }

    private val dataDir: String

    private var isCancelled = true
    private var timestamp = 0L

    private val speechConfig = SpeechConfig.fromSubscription(
        BuildConfig.AZURE_API_KEY,
        BuildConfig.AZURE_REGION
    )

    fun startListening() {
        isCancelled = false
        timestamp = System.currentTimeMillis()

        val intent = Intent(MIC_CENTRAL_REQUEST_ACTION)
        intent.putExtra("timestamp", timestamp)
        context.sendBroadcast(intent)
    }

    fun stopListening() {
        isCancelled = true
        timestamp = 0L

        callback.onStop()
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (isCancelled) return

        val path: String? = intent?.extras?.getString("path")
        if (path != null && path == "$timestamp.wav")
            initiateSTT()
    }

    private fun initiateSTT() {
        thread(true) {
            try {
                callback.onSpeechEnd()

                val uri = "$dataDir/$timestamp.wav"
                val audioConfig = AudioConfig.fromWavFileInput(uri)

                val recognizer = SpeechRecognizer(speechConfig, audioConfig)
                val task = recognizer.recognizeOnceAsync()
                callback.onResult(task.get().text)
            } catch (e: Exception) {
                callback.onResult("")
                Log.d(TAG, "Failure: STT")
                e.printStackTrace()
            } finally {
                callback.onFinal()
            }
        }
    }

    init {
        val packageName = MIC_CENTRAL_PKG
        val pkgManager = context.packageManager
        val appInfo = pkgManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        dataDir = "${appInfo.dataDir}/files"
    }

    companion object {
        const val TAG = "VA.MySpeechRecognizer"

        const val MIC_CENTRAL_PKG = "com.example.miccentral"
        const val MIC_CENTRAL_REQUEST_ACTION = "MIC_CENTRAL_REC_REQUEST"
    }
}
