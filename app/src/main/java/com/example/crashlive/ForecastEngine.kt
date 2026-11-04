package com.example.crashlive

import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/** Descriptive statistics only: past Crash rounds do not predict the next outcome. */
object ForecastEngine {
    private data class Estimate(val p: Double, val low: Double, val high: Double)

    fun buildReport(values: List<Float>): String {
        val clean = values.filter { it.isFinite() && it >= 1f && it <= 100000f }.take(300)
        if (clean.size < 30) {
            return "نمونه معتبر: ${clean.size}/30\nداده کافی نیست؛ فعلاً تخمین ارائه نمی‌شود.\nنتایج Crash از روی دورهای قبل قابل پیش‌بینی قطعی نیست."
        }
        val lines = listOf(1.5f, 2f, 5f).map { threshold ->
            val e = estimate(clean, threshold)
            String.format(Locale.US, "فراوانی تاریخیِ ≥%.1fx: %.1f%% (بازه تقریبی %.1f تا %.1f%%)",
                threshold, e.p * 100, e.low * 100, e.high * 100)
        }
        val recent = clean.take(30).sorted()
        val median = if (recent.size % 2 == 0)
            (recent[recent.size / 2 - 1] + recent[recent.size / 2]) / 2f
        else recent[recent.size / 2]
        return "تحلیل آماری ${clean.size} نمونه اخیر\n" + lines.joinToString("\n") +
            String.format(Locale.US, "\nمیانه ۳۰ نتیجه اخیر: %.2fx", median) +
            "\nاین آمار توصیف گذشته است، نه پیش‌بینی قابل اتکای دور بعد."
    }

    private fun estimate(values: List<Float>, threshold: Float): Estimate {
        val halfLife = 80.0
        var sumW = 0.0
        var sumW2 = 0.0
        var hitW = 0.0
        values.forEachIndexed { index, value ->
            val w = exp(-ln(2.0) * index / halfLife)
            sumW += w
            sumW2 += w * w
            if (value >= threshold) hitW += w
        }
        val p = hitW / sumW
        val n = sumW * sumW / sumW2
        val z = 1.96
        val den = 1.0 + z * z / n
        val center = (p + z * z / (2.0 * n)) / den
        val margin = z * sqrt(p * (1.0 - p) / n + z * z / (4.0 * n * n)) / den
        return Estimate(p, (center - margin).coerceAtLeast(0.0), (center + margin).coerceAtMost(1.0))
    }
}
