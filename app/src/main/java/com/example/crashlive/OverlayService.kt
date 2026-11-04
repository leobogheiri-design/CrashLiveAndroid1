package com.example.crashlive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var panel: LinearLayout? = null
    private var summary: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var minimized = false
    private var alphaLevel = 230
    private val handler = Handler()

    private val refresh = object : Runnable {
        override fun run() {
            updateSummary()
            handler.postDelayed(this, 1200)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
        handler.post(refresh)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun startAsForeground() {
        val channelId = "crash_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "پنل شناور Crash", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }.setContentTitle("پنل شناور Crash فعال است")
            .setContentText("برای بستن پنل، از دکمه بستن داخل آن استفاده کن.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
        startForeground(18, notification)
    }

    private fun showPanel() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 10)
            setBackgroundColor(Color.argb(alphaLevel, 18, 25, 40))
        }

        val heading = TextView(this).apply {
            text = "CRASH LIVE  •  پنل شناور"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 6)
        }
        summary = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.WHITE)
            setPadding(0, 4, 0, 6)
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fun control(label: String, action: () -> Unit): TextView =
            TextView(this).apply {
                text = label
                textSize = 11f
                setTextColor(Color.rgb(180, 220, 255))
                setPadding(5, 8, 9, 4)
                setOnClickListener { action() }
            }

        val minimizeButton = control("جمع/باز", ::toggleMinimized)
        val opacityButton = control("شفافیت", ::cycleOpacity)
        val closeButton = control("بستن ×") { stopSelf() }
        controls.addView(minimizeButton)
        controls.addView(opacityButton)
        controls.addView(closeButton)

        box.addView(heading)
        box.addView(summary)
        box.addView(controls)

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 18
            y = 140
            alpha = 1f
        }

        // Drag the panel by touching its heading.
        heading.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0
            private var startY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = layoutParams.x
                        startY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = startX + (event.rawX - touchX).toInt()
                        layoutParams.y = startY + (event.rawY - touchY).toInt()
                        try { windowManager.updateViewLayout(box, layoutParams) } catch (_: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> return true
                }
                return false
            }
        })

        panel = box
        params = layoutParams
        windowManager.addView(box, layoutParams)
    }

    private fun toggleMinimized() {
        minimized = !minimized
        summary?.visibility = if (minimized) View.GONE else View.VISIBLE
    }

    private fun cycleOpacity() {
        alphaLevel = when {
            alphaLevel >= 230 -> 175
            alphaLevel >= 175 -> 120
            else -> 230
        }
        panel?.setBackgroundColor(Color.argb(alphaLevel, 18, 25, 40))
    }

    private fun updateSummary() {
        val prefs = getSharedPreferences("history", MODE_PRIVATE)
        val raw = prefs.getString("values", "") ?: ""
        val values = raw.split("\n").mapNotNull { line ->
            line.trim().lowercase(Locale.US).removeSuffix("x").replace(',', '.').toFloatOrNull()
        }.filter { it >= 1f }.take(300)

        val captureStatus = prefs.getString("capture_status", "وضعیت بررسی صفحه: شروع نشده") ?: ""
        if (values.isEmpty()) {
            summary?.text = "هنوز ضریبی ثبت نشده.\nبررسی صفحه را از برنامه شروع کن.\n$captureStatus"
            return
        }

        fun rate(threshold: Float): String {
            val hits = values.count { it >= threshold }
            val pct = hits * 100f / values.size
            return String.format(Locale.US, "%.1fx+: %.1f%%", threshold, pct)
        }

        summary?.text = "تعداد نمونه تاریخی: ${values.size}\n" +
            rate(1.5f) + "   " + rate(2f) + "\n" + rate(5f) +
            "\n$captureStatus\nاین آمار نتیجه راند بعد را پیش‌بینی نمی‌کند."
    }

    override fun onDestroy() {
        handler.removeCallbacks(refresh)
        panel?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        panel = null
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
