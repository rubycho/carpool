package com.example.hiemotion

import android.Manifest.permission.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()
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
