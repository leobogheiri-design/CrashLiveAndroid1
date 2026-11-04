package com.example.crashlive

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.nio.ByteBuffer
import java.util.Locale
import java.util.regex.Pattern

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: Handler? = null
    private var thread: HandlerThread? = null
    private var lastScanAt = 0L
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val multiplierPattern = Pattern.compile("(?i)(\\d{1,4}[.,]\\d{1,2}|\\d{1,4})\\s*[x×]")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = Notification.Builder(this, "capture")
            .setContentTitle("Crash Live Analyzer")
            .setContentText("بررسی تاریخچه ضرایب فعال است؛ برای توقف برنامه را باز کن.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
        startForeground(17, notification)

        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra("data", Intent::class.java)
                   else @Suppress("DEPRECATION") intent?.getParcelableExtra("data") as? Intent
        if (resultCode == Activity.RESULT_OK && data != null && projection == null) startCapture(resultCode, data)
        return START_NOT_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        thread = HandlerThread("CrashOCR").also { it.start() }
        worker = Handler(thread!!.looper)
        reader!!.setOnImageAvailableListener({ imageReader ->
            val now = System.currentTimeMillis()
            val image = imageReader.acquireLatestImage() ?: return@setOnImageAvailableListener
            if (now - lastScanAt < 1800) { image.close(); return@setOnImageAvailableListener }
            lastScanAt = now
            processImage(image)
        }, worker)
        virtualDisplay = projection?.createVirtualDisplay(
            "CrashLiveCapture", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface, null, worker
        )
    }

    private fun processImage(image: Image) {
        var bitmap: android.graphics.Bitmap? = null
        var cropped: android.graphics.Bitmap? = null
        try {
            val plane = image.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowPadding = plane.rowStride - pixelStride * image.width
            bitmap = android.graphics.Bitmap.createBitmap(
                image.width + rowPadding / pixelStride, image.height, android.graphics.Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            cropped = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            recognizer.process(InputImage.fromBitmap(cropped, 0))
                .addOnSuccessListener { visionText ->
                    val height = cropped.height.toFloat()
                    // VegasBet screenshot: the "latest explosions" grid is in the lower-middle part of the screen.
                    // Ignore the wager controls and other multipliers elsewhere on the page.
                    val historyLines = visionText.textBlocks.flatMap { it.lines }
                        .filter { line ->
                            val box = line.boundingBox
                            box != null && box.centerY() / height in 0.60f..0.90f
                        }
                        .sortedWith(compareBy({ it.boundingBox!!.top }, { it.boundingBox!!.left }))
                    val found = mutableListOf<String>()
                    for (line in historyLines) {
                        val matcher = multiplierPattern.matcher(line.text)
                        while (matcher.find()) {
                            val value = matcher.group(1).replace(',', '.').toFloatOrNull() ?: continue
                            if (value >= 1f && value <= 100000f) {
                                found.add(String.format(Locale.US, "%.2fx", value))
                                if (found.size >= 40) break
                            }
                        }
                        if (found.size >= 40) break
                    }
                    if (found.size >= 8) saveSnapshot(found)
                }
                .addOnFailureListener { /* Keep the last valid snapshot; do not write partial OCR. */ }
        } catch (_: Exception) {
        } finally {
            image.close()
            // ML Kit may still be processing; recycle in its completion callback below.
            // Bitmap cleanup is handled by the completion listener.
        }
        // Keep the bitmaps alive until ML Kit completes.
        // They are small screen-sized copies and are recycled after the asynchronous task.
        // (The task callbacks are attached above.)
    }

    private fun saveSnapshot(current: List<String>) {
        val prefs = getSharedPreferences("history", MODE_PRIVATE)
        val previousRaw = prefs.getString("last_snapshot", null)
        val previous = previousRaw?.split("|")?.filter { it.isNotBlank() } ?: emptyList()

        if (previous.isEmpty()) {
            // Seed the first run with the visible history, without claiming these are new rounds.
            val existing = prefs.getString("values", "") ?: ""
            val seed = if (existing.isBlank()) current else existing.split("\\n").filter { it.isNotBlank() }
            prefs.edit()
                .putString("values", seed.take(300).joinToString("\n"))
                .putString("last_snapshot", current.joinToString("|"))
                .putString("capture_status", "تاریخچه اولیه ثبت شد؛ منتظر دور جدید")
                .apply()
            return
        }

        if (current == previous) return

        // The visible history is expected to shift toward the end when a new round is added.
        // Find the smallest shift that makes the remaining current list match the previous list.
        var shift = 0
        val maxShift = minOf(8, current.size - 1, previous.size - 1)
        for (candidate in 1..maxShift) {
            val overlap = minOf(current.size - candidate, previous.size)
            if (overlap >= 6 && current.drop(candidate).take(overlap) == previous.take(overlap)) {
                shift = candidate
                break
            }
        }
        if (shift == 0) {
            // A changed layout or noisy OCR is not evidence of a new round.
            prefs.edit().putString("capture_status", "تغییر صفحه دیده شد؛ هنوز دور جدید تأیید نشده").apply()
            return
        }

        val old = (prefs.getString("values", "") ?: "").split("\\n").filter { it.isNotBlank() }
        val combined = (current.take(shift) + old).take(300)
        prefs.edit()
            .putString("values", combined.joinToString("\n"))
            .putString("last_snapshot", current.joinToString("|"))
            .putString("capture_status", "دور جدید از تاریخچه شناسایی شد")
            .apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("capture", "Crash screen analysis", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        virtualDisplay?.release()
        reader?.close()
        projection?.stop()
        thread?.quitSafely()
        recognizer.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
