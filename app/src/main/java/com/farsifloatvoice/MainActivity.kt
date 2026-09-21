package com.farsifloatvoice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.Secure
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val micRequest = 7001
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 52, 36, 36)
        }

        root.addView(TextView(this).apply {
            text = "Farsi Float Voice\n\nحباب شناور برای تبدیل سریع صدای فارسی به متن"
            textSize = 24f
            gravity = Gravity.CENTER
        })

        status = TextView(this).apply {
            textSize = 16f
            setPadding(0, 30, 0, 24)
            movementMethod = ScrollingMovementMethod()
        }
        root.addView(status)

        root.addView(button("🎙 فعال‌سازی میکروفون") { requestMic() })
        root.addView(button("🪟 اجازه نمایش روی برنامه‌ها") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        root.addView(button("♿ فعال‌سازی تایپ مستقیم") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        root.addView(button("🔓 اگر Accessibility محدود است: اطلاعات برنامه") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        })
        root.addView(button("🚀 روشن کردن حباب") { startBubble() })

        setContentView(root)
        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 15f
            setOnClickListener { action() }
            minimumHeight = 56
        }

    private fun requestMic() {
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                micRequest
            )
        }
    }

    private fun startBubble() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestMic()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }

        val intent = Intent(this, FloatingVoiceService::class.java)
        ContextCompat.startForegroundService(this, intent)
        updateStatus()
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Secure.getString(
            contentResolver,
            Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any {
            it.equals("$packageName/.AccessibilityTypingService", ignoreCase = true) ||
                it.equals("$packageName/com.farsifloatvoice.AccessibilityTypingService", ignoreCase = true)
        }
    }

    private fun updateStatus() {
        val mic = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val overlay = Settings.canDrawOverlays(this)
        val access = isAccessibilityEnabled()

        status.text = buildString {
            append("وضعیت:\n")
            append(if (mic) "✅ میکروفون آماده است\n" else "❌ میکروفون فعال نیست\n")
            append(if (overlay) "✅ حباب شناور مجاز است\n" else "❌ نمایش روی برنامه‌ها خاموش است\n")
            append(if (access) "✅ تایپ مستقیم فعال است" else "❌ تایپ مستقیم فعال نیست")
            if (!access) {
                append("\n\nاگر پیام «تنظیم محدود شده» دیدی: وارد «اطلاعات برنامه» شو → منوی ⋮ → «اجازه تنظیمات محدودشده / Allow restricted settings» را فعال کن؛ سپس دوباره Accessibility را باز کن.")
            }
        }
    }
}
