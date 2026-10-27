package com.example.crashlive

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class ChickenActivity : Activity() {
    private lateinit var chanceInput: EditText
    private lateinit var stepsInput: EditText
    private lateinit var result: TextView

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
            text = "تحلیل ریسک بازی مرغ"
            textSize = 23f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(20,40,80)); setPadding(0,8,0,20)
        })
        content.addView(label("احتمال موفقیت هر مرحله (درصد)"))
        chanceInput = input("مثلاً 90", "90")
        content.addView(chanceInput, params())
        content.addView(label("چند مرحله می‌خواهی پشت‌سرهم رد کنی؟"))
        stepsInput = input("مثلاً 5", "5")
        content.addView(stepsInput, params())
        content.addView(Button(this).apply {
            text = "محاسبه ریسک"
            setOnClickListener { calculate() }
        }, params())
        result = TextView(this).apply {
            textSize = 17f; setTextColor(Color.rgb(35,45,60)); setPadding(0,22,0,18)
        }
        content.addView(result)
        content.addView(TextView(this).apply {
            text = "این محاسبه فقط وقتی معتبر است که درصد واردشده واقعاً احتمال هر مرحله باشد و احتمال مراحل مستقل یا به‌درستی مدل شده باشد. اگر بازی احتمال واقعی را اعلام نمی‌کند، نتیجه صرفاً سناریوی فرضی است؛ نتیجه بعدی را پیش‌بینی نمی‌کند."
            textSize = 14f; setTextColor(Color.DKGRAY); setPadding(0,12,0,12)
        })
        content.addView(Button(this).apply { text = "بازگشت"; setOnClickListener { finish() } }, params())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        calculate()
    }

    private fun label(s: String) = TextView(this).apply {
        text=s; textSize=16f; setTextColor(Color.rgb(35,45,60)); setPadding(0,12,0,4)
    }
    private fun input(h: String, v: String) = EditText(this).apply {
        hint=h; setText(v); textSize=17f; singleLine=true
        inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        setPadding(14,10,14,10); setBackgroundColor(Color.WHITE)
    }
    private fun params() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin=8 }

    private fun calculate() {
        val p = chanceInput.text.toString().replace(',', '.').toDoubleOrNull()
        val n = stepsInput.text.toString().toIntOrNull()
        if (p == null || p <= 0.0 || p > 100.0 || n == null || n < 1 || n > 100) {
            result.text = "درصد را بین صفر و 100 و تعداد مراحل را بین 1 تا 100 وارد کن."
            return
        }
        val perStep = p / 100.0
        val survival = Math.pow(perStep, n.toDouble())
        val fail = 1.0 - survival
        val fairGross = 1.0 / survival
        result.text = "احتمال عبور از همه $n مرحله: " + String.format(Locale.US, "%.4f", survival * 100.0) + "%" +
            "\nاحتمال شکست در مسیر: " + String.format(Locale.US, "%.4f", fail * 100.0) + "%" +
            "\nضریب ناخالصِ سربه‌سرِ نظری: " + String.format(Locale.US, "%.3f", fairGross) + "x" +
            "\n\nاین ضریب، سود واقعی یا پیشنهاد شرط نیست؛ ضریب پرداخت بازی ممکن است کمتر باشد و مزیت کازینو داشته باشد."
    }
}
