package com.example.hiemotion

import android.Manifest.permission.*
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()

        val intent = Intent(this, TrackerService::class.java)
        ContextCompat.startForegroundService(this, intent)
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
