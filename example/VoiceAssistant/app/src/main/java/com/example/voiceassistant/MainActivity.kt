package com.example.voiceassistant

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.example.voiceassistant.NotificationListener.Companion.FAKE_BINDER_ACTION
import com.justai.aimybox.Aimybox
import com.justai.aimybox.components.AimyboxAssistantFragment
import kotlinx.coroutines.*
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "VA.MainActivity"
    }

    private lateinit var aimybox: Aimybox

    private var mBounded = false
    private var mService: NotificationListener? = null

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Toast.makeText(this@MainActivity, "Service is connected", Toast.LENGTH_SHORT).show()
            mBounded = true
            mService = (service as NotificationListener.LocalBinder).instance
            mService!!.readAllNotifications()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Toast.makeText(this@MainActivity, "Service is disconnected", Toast.LENGTH_SHORT).show()
            mBounded = false
            mService = null
        }
    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.assistant_container, AimyboxAssistantFragment())
            commit()
        }

        /* Request notification related permission.
           currently user unfriendly */
        if (!permissionGranted()) {
            val intent = Intent(
                "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
            )
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()

        val mIntent = Intent(this, NotificationListener::class.java)
        mIntent.action = FAKE_BINDER_ACTION
        bindService(mIntent, mConnection, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        if (mBounded) {
            unbindService(mConnection)
            mBounded = false
        }
    }

    private fun permissionGranted(): Boolean {
        val sets = NotificationManagerCompat.getEnabledListenerPackages(this)
        return sets.contains(packageName)
    }
}
