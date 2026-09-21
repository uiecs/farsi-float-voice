package com.farsifloatvoice

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {
    private val micRequest = 7001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "Farsi Float Voice\n\nتایپ صوتی فارسی با حباب شناور"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "۱) اجازه میکروفون را بده\n۲) اجازه نمایش روی برنامه‌ها را بده\n۳) سرویس دسترسی‌پذیری را فعال کن\n۴) حباب را روشن کن و هرجا خواستی صحبت کن."
            textSize = 17f
            setPadding(0, 32, 0, 32)
        })
        root.addView(Button(this).apply {
            text = "اجازه میکروفون"
            setOnClickListener { requestMic() }
        })
        root.addView(Button(this).apply {
            text = "اجازه نمایش روی برنامه‌ها"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })
        root.addView(Button(this).apply {
            text = "فعال‌کردن دسترسی تایپ در برنامه‌ها"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "روشن / خاموش کردن حباب شناور"
            setOnClickListener { startService(Intent(this@MainActivity, FloatingVoiceService::class.java)) }
        })
        setContentView(root)
    }

    private fun requestMic() {
        if (Build.VERSION.SDK_INT >= 23) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
        }
    }
}
