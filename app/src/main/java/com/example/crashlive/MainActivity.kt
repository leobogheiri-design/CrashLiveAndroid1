package com.example.crashlive

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.Gravity
import android.graphics.Color

class MainActivity : Activity() {
    private val requestCapture = 4102
    private lateinit var status: TextView
    private lateinit var results: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(246, 248, 252))
        }
        val title = TextView(this).apply {
            text = "Crash Live Analyzer"
            textSize = 24f
            setTextColor(Color.rgb(20, 40, 80))
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "برای شروع، اجازه ضبط صفحه را بده."
            textSize = 16f
            setPadding(0, 18, 0, 18)
        }
        val start = Button(this).apply {
            text = "شروع بررسی زنده"
            setOnClickListener {
                val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(mgr.createScreenCaptureIntent(), requestCapture)
            }
        }
        val stop = Button(this).apply {
            text = "توقف بررسی"
            setOnClickListener {
                stopService(Intent(this@MainActivity, CaptureService::class.java))
                status.text = "بررسی متوقف شد."
            }
        }
        val clear = Button(this).apply {
            text = "پاک کردن تاریخچه"
            setOnClickListener {
                getSharedPreferences("history", MODE_PRIVATE).edit().clear().apply()
                results.text = "تاریخچه پاک شد."
            }
        }
        val mines = Button(this).apply {
            text = "تحلیل احتمال الماس و بمب"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, MinesActivity::class.java))
            }
        }
        val blackjack = Button(this).apply {
            text = "راهنمای بلک‌جک"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, BlackjackActivity::class.java))
            }
        }
        val chicken = Button(this).apply {
            text = "محاسبه ریسک بازی مرغ"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ChickenActivity::class.java))
            }
        }
        val shellGame = Button(this).apply {
            text = "ردیاب گول یا پوچ"
            setOnClickListener {
                startActivity(Intent(this@MainActivity, ShellGameActivity::class.java))
            }
        }
        results = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, 14, 0, 14)
            text = getSharedPreferences("history", MODE_PRIVATE).getString("values", "هنوز ضریبی ثبت نشده.") ?: ""
        }
        root.addView(title)
        root.addView(status)
        root.addView(start)
        root.addView(stop)
        root.addView(clear)
        root.addView(mines)
        root.addView(blackjack)
        root.addView(chicken)
        root.addView(shellGame)
        root.addView(TextView(this).apply { text = "ضرایب شناسایی‌شده"; textSize = 19f; setPadding(0,16,0,6) })
        val scroll = ScrollView(this)
        scroll.addView(results)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    @Deprecated("Deprecated in Android API but compatible with Android 8.1")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCapture && resultCode == RESULT_OK && data != null) {
            val service = Intent(this, CaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            startForegroundService(service)
            status.text = "بررسی زنده فعال است. بازی را باز کن؛ اعلان برنامه را می‌بینی."
        } else if (requestCode == requestCapture) {
            status.text = "اجازه ضبط صفحه داده نشد."
        }
    }

    override fun onResume() {
        super.onResume()
        if (::results.isInitialized) {
            results.text = getSharedPreferences("history", MODE_PRIVATE).getString("values", "هنوز ضریبی ثبت نشده.") ?: ""
        }
    }
}
