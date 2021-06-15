package com.example.carpool

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun start(v: View) {
        ContextCompat.startForegroundService(this, Intent(this, CARPoolManager::class.java))
    }

    fun stop(v: View) {
        applicationContext.stopService(Intent(this, CARPoolManager::class.java))
    }
}
