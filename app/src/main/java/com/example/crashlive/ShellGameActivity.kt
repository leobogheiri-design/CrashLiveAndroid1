package com.example.crashlive

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ShellGameActivity : Activity() {
    private var ballCup = 1
    private var initialized = false
    private var moveCount = 0
    private lateinit var status: TextView
    private lateinit var history: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(246, 248, 252))
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(TextView(this).apply {
            text = "ردیاب گول یا پوچ"
            textSize = 23f
            setTextColor(Color.rgb(20, 40, 80))
            setPadding(0, 8, 0, 16)
        })
        content.addView(TextView(this).apply {
            text = "جای توپ را زمانی انتخاب کن که آن را دیده‌ای. بعد از هر جابه‌جایی، دو لیوانی را که واقعاً عوض شدند ثبت کن."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 12)
        })
        content.addView(TextView(this).apply { text = "جای اولیه توپ"; textSize = 17f })
        val starts = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (cup in 1..3) {
            starts.addView(Button(this).apply {
                text = "لیوان $cup"
                setOnClickListener {
                    ballCup = cup
                    initialized = true
                    moveCount = 0
                    history.text = "جای اولیه: لیوان $cup"
                    updateStatus()
                }
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        content.addView(starts)
        content.addView(TextView(this).apply {
            text = "ثبت جابه‌جایی مشاهده‌شده"
            textSize = 17f
            setPadding(0, 18, 0, 4)
        })
        addSwapButton(content, 1, 2)
        addSwapButton(content, 1, 3)
        addSwapButton(content, 2, 3)
        status = TextView(this).apply {
            textSize = 20f
            setTextColor(Color.rgb(20, 90, 55))
            setPadding(0, 20, 0, 14)
        }
        content.addView(status)
        content.addView(TextView(this).apply {
            text = "تاریخچه جابه‌جایی‌ها"
            textSize = 17f
            setPadding(0, 8, 0, 4)
        })
        history = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.DKGRAY)
            text = "هنوز حرکتی ثبت نشده."
        }
        content.addView(history)
        content.addView(TextView(this).apply {
            text = "محدودیت: برنامه حرکت لیوان‌ها را خودش از ویدیو تشخیص نمی‌دهد. اگر حرکت را گم کنی یا توپ از دید خارج شود، محل تخمینی اشتباه می‌شود. این ابزار نتیجه پنهان را هک یا تضمین نمی‌کند."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 18, 0, 12)
        })
        content.addView(Button(this).apply {
            text = "شروع دوباره"
            setOnClickListener {
                initialized = false
                moveCount = 0
                ballCup = 1
                history.text = "هنوز حرکتی ثبت نشده."
                status.text = "اول جای اولیه توپ را انتخاب کن."
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(Button(this).apply {
            text = "بازگشت"
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        status.text = "اول جای اولیه توپ را انتخاب کن."
    }

    private fun addSwapButton(parent: LinearLayout, first: Int, second: Int) {
        parent.addView(Button(this).apply {
            text = "جابه‌جایی لیوان $first و $second"
            setOnClickListener {
                if (!initialized) {
                    status.text = "ابتدا جای اولیه توپ را انتخاب کن."
                } else {
                    if (ballCup == first) ballCup = second
                    else if (ballCup == second) ballCup = first
                    moveCount++
                    val previous = history.text.toString()
                    val entry = "$moveCount) جابه‌جایی $first ↔ $second"
                    history.text = if (previous == "هنوز حرکتی ثبت نشده.") entry else "$previous\n$entry"
                    updateStatus()
                }
            }
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun updateStatus() {
        status.text = "طبق حرکت‌هایی که ثبت کرده‌ای، توپ در لیوان $ballCup است. حرکت‌های ثبت‌شده: $moveCount"
    }
}
