package com.example.voiceassistant

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.RECORD_AUDIO
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.Notification.EXTRA_TEXT
import android.app.Notification.EXTRA_TITLE
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.os.VibrationEffect.DEFAULT_AMPLITUDE
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_FLUSH
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var speechRecognizer: SpeechRecognizer
    lateinit var textToSpeech: TextToSpeech

    lateinit var chatScrollView: ScrollView
    lateinit var chatContainer: LinearLayout
    lateinit var micButton: ImageButton

    lateinit var requestQueue: RequestQueue

    var service: NotificationService? = null
    var bounded = false

    val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            bounded = true
            service = (binder as NotificationService.LocalBinder).instance

            checkPendingIntent()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bounded = false
            service = null
        }
    }

    private var isListening = false
    private var isPending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()
        lockScreenSetup()

        chatScrollView = findViewById(R.id.chatScrollView)
        chatContainer = findViewById(R.id.chatContainer)
        micButton = findViewById(R.id.micButton)

        requestQueue = Volley.newRequestQueue(this)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.ENGLISH
                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        if (isPending) micEnabled()
                    }

                    override fun onError(utteranceId: String?) {}
                })
            }
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                micDisabled()
            }

            override fun onError(error: Int) {
                micDisabled()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (isPending) {
                    if (matches.isNullOrEmpty()) {
                        cancelPendingIntent(); return
                    }

                    val reply = matches.first()
                    var response = ""
                    if (reply.startsWith("yes")) {
                        response = "OK, I'll respond as %s.".format(reply.removePrefix("yes "))
                        textToSpeech.speak(response, TextToSpeech.QUEUE_FLUSH, null, "randomId3")
                    } else if (reply.startsWith("no")) {
                        response = "OK, I'll not bother you."
                        textToSpeech.speak(response, QUEUE_FLUSH, null, "randomId4")
                    } else sendRequest(reply)

                    addScrollViewItem(reply)
                    if (response.isNotEmpty()) addScrollViewItem(response, true)

                    cancelPendingIntent()
                    return
                }

                if (!matches.isNullOrEmpty()) {
                    addScrollViewItem(matches.first())
                    sendRequest(matches.first())
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        micButton.setOnClickListener {
            if (!isListening) micEnabled()
            else micDisabled()
        }
    }

    override fun onStart() {
        super.onStart()

        val intent = Intent(this, NotificationService::class.java)
        intent.action = FAKE_BINDER_ACTION
        bindService(intent, connection, BIND_AUTO_CREATE)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        checkPendingIntent()
    }

    fun vibrate(duration: Long) {
        val vib = getSystemService(VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vib.vibrate(VibrationEffect.createOneShot(duration, DEFAULT_AMPLITUDE))
        else vib.vibrate(duration)
    }

    fun checkPendingIntent() {
        if (!bounded) return
        if (!service!!.pendingNotificationExists) return

        val statusBarNotification = service!!.pendingNotification!!
        val extras = statusBarNotification.notification.extras

        val person = extras.getCharSequence(EXTRA_TITLE)
        val content = extras.getCharSequence(EXTRA_TEXT)

        isPending = true
        val question =
            "You haven't replied to %s. %s. Would you like to reply?".format(person, content)
        vibrate(1000)
        textToSpeech.speak(question, QUEUE_FLUSH, null, "randomId2")
        addScrollViewItem(question, true)
    }

    fun cancelPendingIntent() {
        isPending = false

        if (!bounded) return
        if (!service!!.pendingNotificationExists) return

        service!!.pendingNotification = null
        service!!.pendingNotificationExists = false
    }

    fun micEnabled() {
        textToSpeech.stop()
        isListening = true

        runOnUiThread {
            speechRecognizer.startListening(createRecognizerIntent(Locale.ENGLISH.toString()))
            micButton.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_red_light
                )
            )
        }
    }

    fun micDisabled() {
        isListening = false

        runOnUiThread {
            speechRecognizer.stopListening()
            micButton.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_green_light
                )
            )
        }
    }

    fun addScrollViewItem(text: String, fromServer: Boolean = false) {
        val layout = if (fromServer) R.layout.message_server
        else R.layout.message_client
        val textView = layoutInflater.inflate(layout, null) as TextView
        textView.text = text

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
        params["unit"] = "1619181413977"

        val request = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        object : JsonObjectRequest(
            Method.POST,
            url, JSONObject(params as Map<*, *>),
            Response.Listener
            {
                val textResponse = it.getString("text")
                textToSpeech.speak(textResponse, TextToSpeech.QUEUE_FLUSH, null, "randomID")
                addScrollViewItem(textResponse, true)
            },
            Response.ErrorListener { }
        ) {
            override fun getBodyContentType(): String = "application/json"
        }
        requestQueue.add(request)
    }

    private fun createRecognizerIntent(language: String) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
        }

    @SuppressLint("InlinedApi")
    private fun checkPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(RECORD_AUDIO, READ_EXTERNAL_STORAGE),
            PERMISSION_GRANTED
        )

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivity(intent)
        }

        val sets = NotificationManagerCompat.getEnabledListenerPackages(this)
        if (!sets.contains(packageName)) {
            val intent = Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }
    }

    fun lockScreenSetup() {
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
}
