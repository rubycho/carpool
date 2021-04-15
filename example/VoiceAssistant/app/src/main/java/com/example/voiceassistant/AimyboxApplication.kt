package com.example.voiceassistant

import com.example.voiceassistant.BuildConfig

import android.app.Application
import android.content.Context

import com.justai.aimybox.Aimybox
import com.justai.aimybox.api.aimybox.AimyboxDialogApi
import com.justai.aimybox.components.AimyboxProvider
import com.justai.aimybox.core.Config
import com.justai.aimybox.speechkit.google.platform.GooglePlatformSpeechToText
import com.justai.aimybox.speechkit.google.platform.GooglePlatformTextToSpeech
// import com.justai.aimybox.speechkit.kaldi.KaldiAssets
// import com.justai.aimybox.speechkit.kaldi.KaldiVoiceTrigger
import java.util.*


class AimyboxApplication : Application(), AimyboxProvider {

    companion object {
        private const val AIMYBOX_API_KEY = BuildConfig.AIMYBOX_API_KEY
    }

    override val aimybox by lazy { createAimybox(this) }

    private fun createAimybox(context: Context): Aimybox {
        val unitId = UUID.randomUUID().toString()

//        val assets = KaldiAssets.fromApkAssets(this, "model/en")
//        val voiceTrigger = KaldiVoiceTrigger(assets, listOf("listen", "hey"))

        val textToSpeech = GooglePlatformTextToSpeech(context, Locale.ENGLISH)
        val speechToText = GooglePlatformSpeechToText(context, Locale.ENGLISH)

        val dialogApi = AimyboxDialogApi(AIMYBOX_API_KEY, unitId)

        return Aimybox(Config.create(speechToText, textToSpeech, dialogApi) {
//            this.voiceTrigger = voiceTrigger
        })
    }
}
