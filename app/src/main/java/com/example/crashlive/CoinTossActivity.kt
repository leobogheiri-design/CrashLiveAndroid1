package com.example.crashlive

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
import kotlin.random.Random

class CoinTossActivity : Activity() {
    private var heads = 0
    private var tails = 0
    private lateinit var result: TextView
    private lateinit var stats: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(246, 248, 252))
        }
        root.addView(TextView(this).apply {
            text = "شیر یا خط"
            textSize = 26f
            setTextColor(Color.rgb(20, 40, 80))
            gravity = Gravity.CENTER
        }, params())
        root.addView(TextView(this).apply {
            text = "هر بار سکه به‌صورت تصادفی شبیه‌سازی می‌شود. آمار فقط نتایج همین نشست را نشان می‌دهد."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 20)
        }, params())
        result = TextView(this).apply {
            text = "برای انداختن سکه دکمه را بزن."
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }
        root.addView(result, params())
        root.addView(Button(this).apply {
            text = "انداختن سکه"
            setOnClickListener { toss() }
        }, params())
        stats = TextView(this).apply {
            textSize = 17f
            setTextColor(Color.rgb(35, 45, 60))
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 20)
        }
        root.addView(stats, params())
        root.addView(Button(this).apply {
            text = "پاک کردن آمار"
            setOnClickListener {
                heads = 0
                tails = 0
                result.text = "آمار پاک شد."
                updateStats()
            }
        }, params())
        root.addView(Button(this).apply {
            text = "بازگشت"
            setOnClickListener { finish() }
        }, params())
        setContentView(root)
        updateStats()
    }

    private fun toss() {
        val isHeads = Random.nextBoolean()
        if (isHeads) heads++ else tails++
        result.text = if (isHeads) "نتیجه: شیر" else "نتیجه: خط"
        updateStats()
    }

    private fun updateStats() {
        val total = heads + tails
        val headPct = if (total == 0) 0.0 else heads * 100.0 / total
        val tailPct = if (total == 0) 0.0 else tails * 100.0 / total
        stats.text = String.format(Locale.US, "تعداد کل: %d\\nشیر: %d (%.1f%%)\\nخط: %d (%.1f%%)", total, heads, headPct, tails, tailPct)
    }

    private fun params() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 10 }
}
