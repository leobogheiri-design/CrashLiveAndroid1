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
import android.text.InputType
import java.util.Locale

class MinesActivity : Activity() {
    private lateinit var cellsInput: EditText
    private lateinit var bombsInput: EditText
    private lateinit var openedInput: EditText
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
        val title = TextView(this).apply {
            text = "تحلیل احتمال الماس و بمب"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(20, 40, 80))
            setPadding(0, 8, 0, 20)
        }
        content.addView(title)
        content.addView(label("تعداد کل خانه‌های صفحه"))
        cellsInput = numberInput("مثلاً 25", "25")
        content.addView(cellsInput, fieldParams())
        content.addView(label("تعداد کل بمب‌ها"))
        bombsInput = numberInput("مثلاً 3", "3")
        content.addView(bombsInput, fieldParams())
        content.addView(label("تعداد خانه‌های امنی که قبلاً باز کرده‌ای"))
        openedInput = numberInput("اگر هنوز خانه‌ای باز نکرده‌ای، 0", "0")
        content.addView(openedInput, fieldParams())

        val calculate = Button(this).apply {
            text = "محاسبه احتمال"
            setOnClickListener { calculateProbability() }
        }
        content.addView(calculate, fieldParams())

        result = TextView(this).apply {
            textSize = 17f
            setTextColor(Color.rgb(35, 45, 60))
            setPadding(0, 22, 0, 18)
            text = "مقادیر صفحه را وارد کن و «محاسبه احتمال» را بزن."
        }
        content.addView(result)

        val note = TextView(this).apply {
            text = "نکته مهم: این محاسبه فرض می‌کند بمب‌ها به‌صورت تصادفی و یکنواخت بین خانه‌ها قرار گرفته‌اند. فقط احتمال ریاضی را نشان می‌دهد؛ محل بمب‌های مخفی را شناسایی نمی‌کند و نتیجه دور بعد را تضمین نمی‌کند."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 12, 0, 12)
        }
        content.addView(note)
        val back = Button(this).apply {
            text = "بازگشت"
            setOnClickListener { finish() }
        }
        content.addView(back, fieldParams())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        calculateProbability()
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 16f
        setTextColor(Color.rgb(35, 45, 60))
        setPadding(0, 12, 0, 4)
    }

    private fun numberInput(hintText: String, initial: String) = EditText(this).apply {
        hint = hintText
        setText(initial)
        textSize = 17f
        inputType = InputType.TYPE_CLASS_NUMBER
        singleLine = true
        contentDescription = hintText
        setPadding(14, 10, 14, 10)
        setBackgroundColor(Color.WHITE)
    }

    private fun fieldParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 8 }

    private fun calculateProbability() {
        val cells = cellsInput.text.toString().toIntOrNull()
        val bombs = bombsInput.text.toString().toIntOrNull()
        val opened = openedInput.text.toString().toIntOrNull()

        if (cells == null || bombs == null || opened == null ||
            cells < 1 || bombs < 0 || bombs >= cells || opened < 0 ||
            opened > cells - bombs || opened >= cells) {
            result.text = "ورودی نامعتبر است. تعداد خانه‌ها باید بیشتر از صفر باشد؛ تعداد بمب‌ها باید کمتر از کل خانه‌ها باشد و خانه‌های امن بازشده هم در محدوده معتبر باشند."
            return
        }

        val remainingCells = cells - opened
        val remainingSafe = cells - bombs - opened
        val safeProbability = remainingSafe.toDouble() / remainingCells.toDouble()
        val bombProbability = 1.0 - safeProbability

        val safePercent = String.format(Locale.US, "%.2f", safeProbability * 100.0)
        val bombPercent = String.format(Locale.US, "%.2f", bombProbability * 100.0)

        val consecutive = StringBuilder()
        var chance = 1.0
        var maxSafeChoices = minOf(5, remainingSafe)
        for (i in 0 until maxSafeChoices) {
            chance *= (remainingSafe - i).toDouble() / (remainingCells - i).toDouble()
            consecutive.append("\nاحتمال ").append(i + 1).append(" انتخاب امن پشت‌سرهم: ")
                .append(String.format(Locale.US, "%.2f", chance * 100.0)).append("%")
        }

        result.text = "احتمال امن بودن انتخاب بعدی: $safePercent%\n" +
            "احتمال بمب بودن انتخاب بعدی: $bombPercent%\n" +
            "خانه‌های باقی‌مانده: $remainingCells\n" +
            "خانه‌های امن باقی‌مانده: $remainingSafe" + consecutive.toString()
    }
}
