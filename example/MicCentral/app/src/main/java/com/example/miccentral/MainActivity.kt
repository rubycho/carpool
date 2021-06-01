package com.example.miccentral

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()
   }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
                this, arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ), 0)
    }

    fun onButtonClick(v: View) {
        for (p in PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, p) != PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                return
            }
        }

        ContextCompat.startForegroundService(this, Intent(this, CentralService::class.java))
        finish()
    }

    companion object {
        val PERMISSION_LIST = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
}
