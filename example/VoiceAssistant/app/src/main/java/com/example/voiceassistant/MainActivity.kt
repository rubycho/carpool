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
import com.justai.aimybox.model.TextSpeech
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "com.example.voice_assistant.MainActivity"
    }

    private lateinit var aimybox: Aimybox

    private var mBounded = false
    private var mService: NotificationListener? = null

    private val mTimer = Timer()
    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Toast.makeText(this@MainActivity, "Service is connected", Toast.LENGTH_SHORT).show()
            mBounded = true
            mService = (service as NotificationListener.LocalBinder).instance

            /* TODO: messy code!! */
            mTimer.schedule(
                object : TimerTask() {
                    override fun run() {
                        val notifications = mService!!.activeNotifications
                        Log.d(TAG, "Notification empty(): " + notifications.isNullOrEmpty().toString())

                        if (notifications != null && notifications.isNotEmpty()) {
                            Log.d(TAG, "Notification size: " + notifications.size.toString())
                            notifications.forEachIndexed { idx, item ->
                                Log.d(TAG, "[" + idx + "] " + item.packageName)
                            }
                        }
                    }
                }, 0, 5 * 1000
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Toast.makeText(this@MainActivity, "Service is disconnected", Toast.LENGTH_SHORT).show()
            mBounded = false
            mService = null

            mTimer.cancel()
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

        aimybox = (application as AimyboxApplication).aimybox
        aimybox.speak(TextSpeech("Hi Sung jae!"), Aimybox.NextAction.RECOGNITION)

        /*
            Request notification related permission.
            currently user unfriendly :(
        */
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
