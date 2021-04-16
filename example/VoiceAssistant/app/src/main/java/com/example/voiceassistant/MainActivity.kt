package com.example.voiceassistant

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.justai.aimybox.Aimybox
import com.justai.aimybox.components.AimyboxAssistantFragment
import com.justai.aimybox.model.TextSpeech

class MainActivity : AppCompatActivity() {
    private lateinit var aimybox: Aimybox

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.assistant_container, AimyboxAssistantFragment())
            commit()
        }

        aimybox = (application as AimyboxApplication).aimybox
        aimybox.speak(TextSpeech("Hi Sung jae!"), Aimybox.NextAction.RECOGNITION)
    }
}
