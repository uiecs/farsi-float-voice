package com.farsifloatvoice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast

class FloatingVoiceService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: ImageView? = null
    private var recognizer: SpeechRecognizer? = null
    private var listening = false
    private var downX = 0f
    private var downY = 0f
    private var startX = 0
    private var startY = 0

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        showBubble()
    }

    private fun startForegroundNotification() {
        val channelId = "farsi_float_voice"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Farsi Float Voice", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Farsi Float Voice")
                .setContentText("حباب تایپ صوتی فارسی فعال است")
                .setOngoing(true)
                .build()
        } else {
            Notification.Builder(this)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("Farsi Float Voice")
                .setContentText("حباب تایپ صوتی فارسی فعال است")
                .setOngoing(true)
                .build()
        }
        startForeground(1001, notification)
    }

    private fun showBubble() {
        if (bubble != null) return
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        bubble = ImageView(this).apply {
            setBackgroundColor(Color.argb(185, 20, 20, 24))
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setColorFilter(Color.WHITE)
            setPadding(22, 22, 22, 22)
            contentDescription = "تایپ صوتی فارسی"
            alpha = 0.88f
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        val lp = v.layoutParams as WindowManager.LayoutParams
                        startX = lp.x
                        startY = lp.y
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val lp = v.layoutParams as WindowManager.LayoutParams
                        lp.x = startX + (event.rawX - downX).toInt()
                        lp.y = startY + (event.rawY - downY).toInt()
                        wm.updateViewLayout(v, lp)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val moved = kotlin.math.abs(event.rawX - downX) + kotlin.math.abs(event.rawY - downY)
                        if (moved < 18f) toggleListening()
                        true
                    }
                    else -> true
                }
            }
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val lp = WindowManager.LayoutParams(
            72, 72, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 18
            y = 420
        }
        wm.addView(bubble, lp)
    }

    private fun toggleListening() {
        if (listening) {
            recognizer?.stopListening()
            listening = false
            setBubble(false)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "سرویس تشخیص گفتار روی این گوشی در دسترس نیست", Toast.LENGTH_LONG).show()
            return
        }
        startRecognition()
    }

    private fun startRecognition() {
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "ابتدا اجازه میکروفون را فعال کن", Toast.LENGTH_SHORT).show()
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer!!.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) { listening = true; setBubble(true) }
            override fun onBeginningOfSpeech() { setBubble(true) }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false; setBubble(false) }
            override fun onError(error: Int) {
                listening = false
                setBubble(false)
                if (error != SpeechRecognizer.ERROR_CLIENT && error != SpeechRecognizer.ERROR_NO_MATCH) {
                    Toast.makeText(this@FloatingVoiceService, "تشخیص صدا ناموفق بود؛ دوباره امتحان کن", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResults(results: android.os.Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                if (text.isNotEmpty()) AccessibilityTypingService.insertText(text)
            }
            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        recognizer!!.startListening(intent)
    }

    private fun setBubble(active: Boolean) {
        bubble?.alpha = if (active) 1f else 0.72f
        bubble?.setBackgroundColor(if (active) Color.argb(220, 150, 30, 30) else Color.argb(185, 20, 20, 24))
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        bubble?.let { wm.removeView(it) }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
