package com.example.crashlive

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var summary: TextView? = null
    private val handler = Handler()
    private val refresh = object : Runnable {
        override fun run() {
            updateSummary()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
        handler.post(refresh)
    }

    private fun showPanel() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 14, 18, 14)
            setBackgroundColor(Color.argb(235, 22, 28, 42))
        }
        val heading = TextView(this).apply {
            text = "CRASH LIVE  •  پنل آماری"
            textSize = 14f
            setTextColor(Color.WHITE)
        }
        summary = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(0, 10, 0, 8)
        }
        val close = TextView(this).apply {
            text = "بستن پنل  ×"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.END
            setPadding(0, 8, 0, 0)
            setOnClickListener { stopSelf() }
        }
        box.addView(heading)
        box.addView(summary)
        box.addView(close)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 12
            y = 120
        }
        panel = box
        windowManager.addView(box, params)
    }

    private fun updateSummary() {
        val raw = getSharedPreferences("history", MODE_PRIVATE).getString("values", "") ?: ""
        val values = raw.split("\\n").mapNotNull { line ->
            line.trim().lowercase(Locale.US).removeSuffix("x").replace(',', '.').toFloatOrNull()
        }.filter { it >= 1f }.take(100)
        if (values.isEmpty()) {
            summary?.text = "هنوز داده‌ای ثبت نشده.\\nابتدا بررسی صفحه را فعال کن."
            return
        }
        fun rate(threshold: Float): String {
            val hits = values.count { it >= threshold }
            val pct = hits * 100f / values.size
            return String.format(Locale.US, "%.1fx یا بیشتر: %.1f%%", threshold, pct)
        }
        summary?.text = "نمونه‌های خوانده‌شده: ${values.size}\\n" +
            rate(1.5f) + "\\n" + rate(2f) + "\\n" + rate(5f) +
            "\\n\\nآمار تاریخی است؛ پیش‌بینی دور بعد نیست."
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        panel?.let { windowManager.removeView(it) }
        panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}