package com.example.hiemotion

import android.Manifest.permission.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {
    private lateinit var wavRecorder: WavRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wavRecorder = WavRecorder()
        wavRecorder.startRecording()

        requestPermission()
    }

    override fun onDestroy() {
        super.onDestroy()

        wavRecorder.stopRecording()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                RECORD_AUDIO,
                READ_EXTERNAL_STORAGE,
                WRITE_EXTERNAL_STORAGE
            ), 0
        )
    }
}
