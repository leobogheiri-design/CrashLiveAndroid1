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

class BlackjackActivity : Activity() {
    private lateinit var playerInput: EditText
    private lateinit var dealerInput: EditText
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
            text = "راهنمای بلک‌جک"
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(20, 40, 80))
            setPadding(0, 8, 0, 20)
        })
        content.addView(label("کارت‌های دست خودت (با کاما جدا کن)"))
        playerInput = input("مثلاً A,7 یا 10,6", "10,6")
        content.addView(playerInput, params())
        content.addView(label("کارت نمایان دیلر"))
        dealerInput = input("مثلاً A یا 6 یا K", "6")
        content.addView(dealerInput, params())
        content.addView(Button(this).apply {
            text = "پیشنهاد حرکت"
            setOnClickListener { analyze() }
        }, params())
        result = TextView(this).apply {
            textSize = 17f
            setTextColor(Color.rgb(35, 45, 60))
            setPadding(0, 22, 0, 18)
        }
        content.addView(result)
        content.addView(TextView(this).apply {
            text = "این راهنمای ساده از استراتژی پایه تقریبی برای یک دست معمولی استفاده می‌کند. جفت‌ها و تصمیم Split، قوانین خاص میز، کارت‌های خارج‌شده و تفاوت قوانین دیلر را کامل پوشش نمی‌دهد. پیشنهاد احتمالاتی است، نه تضمین برد."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 12, 0, 12)
        })
        content.addView(Button(this).apply { text = "بازگشت"; setOnClickListener { finish() } }, params())
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        analyze()
    }

    private fun label(value: String) = TextView(this).apply {
        text = value; textSize = 16f; setTextColor(Color.rgb(35,45,60)); setPadding(0,12,0,4)
    }

    private fun input(hintText: String, initial: String) = EditText(this).apply {
        hint = hintText; setText(initial); textSize = 17f; isSingleLine = true
        setPadding(14, 10, 14, 10); setBackgroundColor(Color.WHITE)
    }

    private fun params() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 8 }

    private fun cardValue(raw: String): Int? {
        return when (raw.trim().uppercase(Locale.US)) {
            "A", "ACE" -> 11
            "K", "Q", "J", "KING", "QUEEN", "JACK" -> 10
            else -> raw.trim().toIntOrNull()?.takeIf { it in 2..10 }
        }
    }

    private fun analyze() {
        val cards = playerInput.text.toString().split(",", ";", " ", "،")
            .map { it.trim() }.filter { it.isNotEmpty() }
            .map { cardValue(it) }
        val dealer = cardValue(dealerInput.text.toString())
        if (cards.isEmpty() || cards.any { it == null } || dealer == null) {
            result.text = "ورودی نامعتبر است. کارت‌ها را مثل A,7 یا 10,6 بنویس و کارت دیلر را وارد کن."
            return
        }
        val values = cards.filterNotNull()
        var total = values.sum()
        var acesAsEleven = values.count { it == 11 }
        while (total > 21 && acesAsEleven > 0) {
            total -= 10
            acesAsEleven--
        }
        if (total > 21) {
            result.text = "دست تو $total است؛ این دست Bust شده. این محاسبه برای دست بعدی را انجام بده."
            return
        }

        val soft = values.contains(11) && total <= 21 && values.sum() - 10 * (values.count { it == 11 } - acesAsEleven) <= 21 && acesAsEleven > 0
        val dealerValue = dealer
        val action: String
        val reason: String

        if (soft) {
            when {
                total >= 19 -> { action = "بایست (STAND)"; reason = "دست نرم 19 یا بیشتر داری." }
                total == 18 && dealerValue in 3..6 -> { action = "اگر مجاز است دابل؛ وگرنه بایست"; reason = "دست نرم 18 در برابر کارت ضعیف دیلر." }
                total == 18 && dealerValue in listOf(2, 7, 8) -> { action = "بایست (STAND)"; reason = "دست نرم 18 در برابر این کارت معمولاً می‌ایستد." }
                else -> { action = "کارت بگیر (HIT)"; reason = "دست نرم تو در برابر کارت دیلر معمولاً نیاز به بهبود دارد." }
            }
        } else {
            when {
                total <= 8 -> { action = "کارت بگیر (HIT)"; reason = "جمع کارت‌ها پایین است." }
                total == 9 && dealerValue in 3..6 -> { action = "اگر مجاز است دابل؛ وگرنه کارت بگیر"; reason = "۹ در برابر ۳ تا ۶ موقعیت مناسب دابل است." }
                total == 10 && dealerValue in 2..9 -> { action = "اگر مجاز است دابل؛ وگرنه کارت بگیر"; reason = "۱۰ در برابر ۲ تا ۹ معمولاً دابل می‌شود." }
                total == 11 && dealerValue != 11 -> { action = "اگر مجاز است دابل؛ وگرنه کارت بگیر"; reason = "۱۱ معمولاً موقعیت مناسبی برای دابل است." }
                total == 12 && dealerValue in 4..6 -> { action = "بایست (STAND)"; reason = "۱۲ در برابر ۴ تا ۶ معمولاً می‌ایستد." }
                total in 13..16 && dealerValue in 2..6 -> { action = "بایست (STAND)"; reason = "در برابر کارت ضعیف دیلر، احتمال Bust شدن دیلر مهم است." }
                total >= 17 -> { action = "بایست (STAND)"; reason = "جمع دست ۱۷ یا بیشتر است." }
                else -> { action = "کارت بگیر (HIT)"; reason = "برای بهتر شدن جمع دست، کارت گرفتن معمولاً گزینه پایه است." }
            }
        }

        result.text = "جمع دست تو: $total" + (if (soft) " (Soft)" else " (Hard)") +
            "\nکارت نمایان دیلر: " + if (dealerValue == 11) "A" else dealerValue.toString() +
            "\nپیشنهاد: $action\nدلیل: $reason"
    }
}
