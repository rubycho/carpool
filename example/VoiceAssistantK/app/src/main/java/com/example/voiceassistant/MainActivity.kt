package com.example.voiceassistant

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification.EXTRA_TEXT
import android.app.Notification.EXTRA_TITLE
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.*
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_FLUSH
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.voiceassistant.NotificationService.Companion.FAKE_BINDER_ACTION
import com.example.voiceassistant.NotificationService.Companion.REPLY_SET_EXTRA_KEY
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity() {

    private var appState = AppState.STANDBY

    private lateinit var speechRecognizer: MySpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech

    private lateinit var chatScrollView: ScrollView
    private lateinit var chatContainer: LinearLayout
    private lateinit var micButton: ImageButton

    private lateinit var requestQueue: RequestQueue

    private var service: NotificationService? = null
    private var bounded = false

    private lateinit var handler: Handler
    private val timer = Timer()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d("tid (connection)", Process.myTid().toString())

            bounded = true
            service = (binder as NotificationService.LocalBinder).instance
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bounded = false
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("tid (onCreate)", Process.myTid().toString())

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()
        lockScreenSetup()

        chatScrollView = findViewById(R.id.chatScrollView)
        chatContainer = findViewById(R.id.chatContainer)
        micButton = findViewById(R.id.micButton)

        requestQueue = Volley.newRequestQueue(this)

        val utteranceProgressListener = object: UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {

            }

            override fun onDone(utteranceId: String?) {
                Log.d("tid (TTS)", Process.myTid().toString())

                runOnUiThread {
                    if (appState == AppState.C_ANSWER) {
                        appState = AppState.STANDBY
                        return@runOnUiThread
                    }

                    if (appState == AppState.P_ALERT) {
                        appState = AppState.P_LISTEN
                        micEnabled()
                        return@runOnUiThread
                    }

                    if (appState == AppState.P_ALERT2) {
                        appState = AppState.P_LISTEN2
                        micEnabled()
                        return@runOnUiThread
                    }

                    if (appState == AppState.P_DISMISS) {
                        appState = AppState.STANDBY
                        return@runOnUiThread
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                appState = AppState.STANDBY
            }
        }
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.ENGLISH
                textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener)
            }
        }

        val speechRecognizerCallback = object: MySpeechRecognizer.Callback {
            override fun onSpeechEnd() {
                micDisabled(false)
                micButton.setImageDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_wait))
            }

            override fun onResult(s: String) {
                runOnUiThread {
                    if (appState == AppState.C_LISTEN) {
                        if (s.isNotEmpty()) {
                            addScrollViewItem(s)

                            appState = AppState.C_NETWORK
                            sendRequest(s)
                        }
                        return@runOnUiThread
                    }

                    if (appState == AppState.P_LISTEN) {
                        if (s.isEmpty()) {
                            appState = AppState.STANDBY; return@runOnUiThread
                        }

                        val reply = s
                        var response = ""
                        if (reply.startsWith("yes")) {
                            appState = AppState.P_ALERT2

                            response = "OK. What would be the content?"
                            textToSpeech.speak(response, QUEUE_FLUSH,
                                null, PROACTIVE_CONFIRM_UID)
                        } else if (reply.startsWith("no")) {
                            appState = AppState.P_DISMISS

                            response = "OK, I'll not bother you."
                            textToSpeech.speak(response, QUEUE_FLUSH,
                                null, PROACTIVE_DISMISS_UID)
                        } else {
                            appState = AppState.STANDBY; return@runOnUiThread
                        }

                        addScrollViewItem(reply)
                        addScrollViewItem(response, true)
                        return@runOnUiThread
                    }

                    if (appState == AppState.P_LISTEN2) {
                        if (s.isEmpty()) {
                            appState = AppState.STANDBY; return@runOnUiThread
                        }

                        val content = s
                        val response = "OK. I\'ll respond as %s.".format(content)

                        textToSpeech.speak(response, QUEUE_FLUSH, null, PROACTIVE_DONE_UID)

                        addScrollViewItem(content)
                        addScrollViewItem(response, true)

                        appState = AppState.STANDBY
                        resolveReplyableNotification()
                        return@runOnUiThread
                    }
                }
            }

            override fun onStop() {

            }

            override fun onFinal() {
                runOnUiThread {
                    micButton.setImageDrawable(ContextCompat.getDrawable(
                        this@MainActivity,
                        R.drawable.ic_mic
                    ))
                }
            }
        }
        speechRecognizer = MySpeechRecognizer(this, speechRecognizerCallback)
        registerReceiver(speechRecognizer, IntentFilter(MIC_CENTRAL_RESPONSE_ACTION))

        micButton.setOnClickListener {
            Log.d("tid (onClick)", Process.myTid().toString())

            if (appState == AppState.STANDBY || appState == AppState.C_ANSWER) {
                appState = AppState.C_LISTEN
                micEnabled()
                return@setOnClickListener
            }
            if (appState == AppState.C_LISTEN) {
                appState = AppState.STANDBY
                micDisabled()
                return@setOnClickListener
            }
        }

        handler = Handler(Looper.getMainLooper())
    }

    override fun onStart() {
        super.onStart()

        val intent = Intent(this, NotificationService::class.java).apply {
            action = FAKE_BINDER_ACTION
        }
        bindService(intent, connection, BIND_AUTO_CREATE)

        /* debug purpose */
        timer.scheduleAtFixedRate(object: TimerTask() {
            override fun run() {
                Log.d("APPSTATE", appState.toString())
            }
        }, 0, 500)
    }

    override fun onStop() {
        super.onStop()

        timer.cancel()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.extras?.containsKey(REPLY_SET_EXTRA_KEY) == true)
            checkReplyableNotification()
    }

    fun vibrate(duration: Long) {
        val vib = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vib.vibrate(VibrationEffect.createOneShot(duration, DEFAULT_AMPLITUDE))
        else vib.vibrate(duration)
    }

    fun checkReplyableNotification() {
        if (!bounded) return
        if (!service!!.replyNotificationExists) return

        val statusBarNotification = service!!.replyNotification!!
        val extras = statusBarNotification.notification.extras

        val person = extras.getCharSequence(EXTRA_TITLE)
        val content = extras.getCharSequence(EXTRA_TEXT)

        /**
         * If user is currently classic mode,
         * delay every 1 min, until user exits classic mode.
         * This is a temporary action.
         */
        val stateCheck = object: Runnable {
            override fun run() {
                if (appState != AppState.STANDBY) {
                    Log.d("WAITING", "Proactive delayed.")

                    handler.postDelayed(this, 60 * 1000)
                    return
                }

                appState = AppState.P_ALERT

                val question =
                    "You haven't replied to %s. %s. Would you like to reply?".format(person, content)
                vibrate(1000)
                textToSpeech.speak(question, QUEUE_FLUSH, null, PROACTIVE_START_UID)
                addScrollViewItem(question, true)
                handler.removeCallbacks(this)
            }
        }
        stateCheck.run()
    }

    fun resolveReplyableNotification() {
        if (!bounded) return
        if (!service!!.replyNotificationExists) return

        service!!.unsetReplyNotification()
    }

    fun micEnabled() {
        textToSpeech.stop()

        runOnUiThread {
            speechRecognizer.startListening()
            micButton.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_red_light
                )
            )
        }
    }

    fun micDisabled(byUserRequest: Boolean = true) {
        runOnUiThread {
            if (byUserRequest)
                speechRecognizer.stopListening()
            micButton.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_green_light
                )
            )
        }
    }

    fun addScrollViewItem(_text: String, fromServer: Boolean = false) {
        val layout = if (fromServer) R.layout.message_server
        else R.layout.message_client
        val textView = (layoutInflater.inflate(layout, null) as TextView).apply {
            text = _text
        }

        chatContainer.addView(textView)
        chatScrollView.post {
            chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    fun sendRequest(query: String) {
        val url = "https://api.aimybox.com/request"
        val params = HashMap<String, String>()
        params["query"] = query
        params["key"] = BuildConfig.AIMYBOX_API_KEY
        /**
         * TODO: randomize unit
         */
        params["unit"] = "1619181413977"

        val request = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        object : JsonObjectRequest(
            Method.POST,
            url, JSONObject(params as Map<*, *>),
            Response.Listener
            {
                Log.d("tid (volley)", Process.myTid().toString())

                appState = AppState.C_ANSWER

                val textResponse = it.getString("text")
                textToSpeech.speak(textResponse, TextToSpeech.QUEUE_FLUSH, null, RESPONSE_UID)
                addScrollViewItem(textResponse, true)
            },
            Response.ErrorListener { }
        ) {
            override fun getBodyContentType(): String = "application/json"
        }
        requestQueue.add(request)
    }

    @SuppressLint("InlinedApi")
    private fun checkPermission() {
        /* General permission */
        ActivityCompat.requestPermissions(
            this,
            arrayOf(READ_EXTERNAL_STORAGE),
            PERMISSION_GRANTED
        )

        /* Permission for launching activity when phone is locked */
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        }

        /* Permission to read notifications */
        val sets = NotificationManagerCompat.getEnabledListenerPackages(this)
        if (!sets.contains(packageName)) {
            val intent = Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
    }

    /**
     * Got from stackoverflow
     *https://stackoverflow.com/questions/35356848/android-how-to-launch-activity-over-lock-screen
     */
    private fun lockScreenSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    companion object {
        const val PROACTIVE_START_UID = "START_UTTERANCE"
        const val PROACTIVE_CONFIRM_UID = "CONFIRM_UTTERANCE"
        const val PROACTIVE_DISMISS_UID = "DISMISS_UTTERANCE"
        const val PROACTIVE_DONE_UID = "DONE_UTTERANCE"

        const val RESPONSE_UID = "RESPONSE_UTTERANCE"

        const val MIC_CENTRAL_RESPONSE_ACTION = "MIC_CENTRAL_REC_RESPONSE"
    }
}
