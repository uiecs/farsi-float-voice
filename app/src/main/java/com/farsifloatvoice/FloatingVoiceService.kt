package com.farsifloatvoice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ServiceCompat
import kotlin.math.abs

class FloatingVoiceService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: ImageView? = null
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0
    private var restarting = false

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        showBubble()
    }

    private fun startForegroundNotification() {
        val channelId = "farsi_float_voice"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Farsi Float Voice",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        val notification: Notification =
            if (Build.VERSION.SDK_INT >= 26) {
                Notification.Builder(this, channelId)
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setContentTitle("Farsi Float Voice")
                    .setContentText("حباب شناور آماده است")
                    .setOngoing(true)
                    .build()
            } else {
                Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                    .setContentTitle("Farsi Float Voice")
                    .setContentText("حباب شناور آماده است")
                    .setOngoing(true)
                    .build()
            }

        if (Build.VERSION.SDK_INT >= 29) {
            ServiceCompat.startForeground(
                this,
                1001,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(1001, notification)
        }
    }

    private fun showBubble() {
        if (bubble != null) return
        wm = getSystemService(WINDOW_SERVICE) as WindowManager

        bubble = ImageView(this).apply {
            setBackgroundColor(Color.argb(210, 25, 25, 25))
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setColorFilter(Color.WHITE)
            setPadding(20, 20, 20, 20)
            alpha = 0.9f
            contentDescription = "حباب تایپ صوتی فارسی"

            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        val lp = v.layoutParams as WindowManager.LayoutParams
                        startX = lp.x
                        startY = lp.y
                        dragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (abs(dx) > 8 || abs(dy) > 8) dragging = true
                        val lp = v.layoutParams as WindowManager.LayoutParams
                        lp.x = startX + dx.toInt()
                        lp.y = startY + dy.toInt()
                        wm.updateViewLayout(v, lp)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!dragging) toggleListening()
                        true
                    }
                    else -> true
                }
            }
        }

        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            68,
            68,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 18
            y = 420
        }

        wm.addView(bubble, lp)
    }

    private fun toggleListening() {
        if (listening) stopListening()
        else startListening()
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "اجازه میکروفون فعال نیست", Toast.LENGTH_SHORT).show()
            return
        }

        if (!AccessibilityTypingService.isReady()) {
            Toast.makeText(
                this,
                "ابتدا دسترسی «تایپ مستقیم» را در Accessibility فعال کن",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "سرویس تشخیص گفتار روی دستگاه در دسترس نیست", Toast.LENGTH_LONG).show()
            return
        }

        if (!AccessibilityTypingService.beginDictation()) {
            Toast.makeText(this, "اول یک کادر تایپ را باز و روی آن فوکوس کن", Toast.LENGTH_SHORT).show()
            return
        }

        listening = true
        restarting = false
        setBubble(true)
        startRecognizer()
    }

    private fun startRecognizer() {
        if (!listening) return

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) = Unit
            override fun onBeginningOfSpeech() = setBubble(true)
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                if (listening) restartRecognition()
            }

            override fun onError(error: Int) {
                if (!listening) return
                if (error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                    stopListening()
                } else {
                    restartRecognition()
                }
            }

            override fun onResults(results: android.os.Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
                if (text.isNotEmpty()) {
                    AccessibilityTypingService.updateDictation(text)
                    AccessibilityTypingService.commitDictation()
                }
                if (listening) restartRecognition()
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim().orEmpty()
                if (text.isNotEmpty()) AccessibilityTypingService.updateDictation(text)
            }

            override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        recognizer?.startListening(intent)
    }

    private fun restartRecognition() {
        if (!listening || restarting) return
        restarting = true
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            restarting = false
            if (listening) {
                AccessibilityTypingService.commitDictation()
                startRecognizer()
            }
        }, 180)
    }

    private fun stopListening() {
        listening = false
        restarting = false
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
        AccessibilityTypingService.commitDictation()
        AccessibilityTypingService.cancelDictation()
        setBubble(false)
    }

    private fun setBubble(active: Boolean) {
        bubble?.alpha = if (active) 1f else 0.82f
        bubble?.setBackgroundColor(
            if (active) Color.argb(225, 0, 150, 90)
            else Color.argb(210, 25, 25, 25)
        )
    }

    override fun onDestroy() {
        listening = false
        recognizer?.destroy()
        recognizer = null
        bubble?.let { wm.removeView(it) }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
