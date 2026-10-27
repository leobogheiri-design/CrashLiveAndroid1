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
    private val seenThisSession = linkedSetOf<String>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = Notification.Builder(this, "capture")
            .setContentTitle("Crash Live Analyzer")
            .setContentText("بررسی صفحه فعال است؛ برای توقف برنامه را باز کن.")
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
        val density = metrics.densityDpi
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
            "CrashLiveCapture", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader!!.surface, null, worker
        )
    }

    private fun processImage(image: Image) {
        try {
            val plane = image.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmap = android.graphics.Bitmap.createBitmap(
                image.width + rowPadding / pixelStride, image.height, android.graphics.Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            val cropped = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            val input = InputImage.fromBitmap(cropped, 0)
            recognizer.process(input)
                .addOnSuccessListener { visionText ->
                    val regex = Pattern.compile("(?i)(\\d{1,4}[.,]?\\d{0,2})\\s*[x×]")
                    val matcher = regex.matcher(visionText.text)
                    val found = mutableListOf<String>()
                    while (matcher.find()) {
                        val value = matcher.group(1).replace(',', '.').toFloatOrNull() ?: continue
                        if (value >= 1f && value <= 100000f) {
                            val formatted = String.format(Locale.US, "%.2fx", value)
                            if (found.size < 80) found.add(formatted)
                        }
                    }
                    if (found.isNotEmpty()) saveNew(found)
                }
                .addOnCompleteListener { cropped.recycle(); bitmap.recycle() }
        } catch (_: Exception) {
        } finally {
            image.close()
        }
    }

    private fun saveNew(found: List<String>) {
        val prefs = getSharedPreferences("history", MODE_PRIVATE)
        val existing = prefs.getString("values", "") ?: ""
        val old = if (existing.isBlank()) mutableListOf() else existing.split("\\n").toMutableList()
        // Keep only new OCR detections in the current session; OCR may see the same history repeatedly.
        val newlySeen = found.filter { seenThisSession.add(it) }
        if (newlySeen.isEmpty()) return
        val combined = (newlySeen + old).take(300)
        prefs.edit().putString("values", combined.joinToString("\n")).apply()
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
