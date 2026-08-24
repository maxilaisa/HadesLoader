package com.hadesloader.poolinjector.cue

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class CueDetector(private val context: Context) {
    
    data class CueStats(
        val id: String,
        val name: String,
        val powerMultiplier: Float,
        val spinMultiplier: Float,
        val accuracyMultiplier: Float,
        val forceMultiplier: Float
    )
    
    private val mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var isCapturing = false
    
    // Cue database with actual 8 Ball Pool cue stats
    private val cueDatabase = mapOf(
        "Standard Cue" to CueStats("standard", "Standard Cue", 1.0f, 1.0f, 1.0f, 1.0f),
        "Pro Cue" to CueStats("pro", "Pro Cue", 1.2f, 1.1f, 1.05f, 1.15f),
        "Legendary Cue" to CueStats("legendary", "Legendary Cue", 1.5f, 1.3f, 1.2f, 1.4f),
        "Archangel Cue" to CueStats("archangel", "Archangel Cue", 1.8f, 1.5f, 1.3f, 1.6f),
        "Galaxy Cue" to CueStats("galaxy", "Galaxy Cue", 2.0f, 1.6f, 1.4f, 1.8f),
        "Infinity Cue" to CueStats("infinity", "Infinity Cue", 2.2f, 1.7f, 1.5f, 2.0f),
        "Black Hole Cue" to CueStats("blackhole", "Black Hole Cue", 2.5f, 1.8f, 1.6f, 2.2f),
        "Ice Cue" to CueStats("ice", "Ice Cue", 1.6f, 1.4f, 1.1f, 1.5f),
        "Fire Cue" to CueStats("fire", "Fire Cue", 1.7f, 1.4f, 1.2f, 1.6f),
        "Lightning Cue" to CueStats("lightning", "Lightning Cue", 1.9f, 1.5f, 1.3f, 1.7f),
        "Platinum Cue" to CueStats("platinum", "Platinum Cue", 1.4f, 1.2f, 1.1f, 1.3f),
        "Golden Cue" to CueStats("golden", "Golden Cue", 1.3f, 1.1f, 1.1f, 1.2f),
        "Diamond Cue" to CueStats("diamond", "Diamond Cue", 1.8f, 1.4f, 1.25f, 1.7f),
        "Dragon Cue" to CueStats("dragon", "Dragon Cue", 2.1f, 1.6f, 1.35f, 1.9f),
        "Phoenix Cue" to CueStats("phoenix", "Phoenix Cue", 2.3f, 1.7f, 1.45f, 2.1f)
    )
    
    private var currentCue: CueStats = cueDatabase["Standard Cue"]!!
    private var onCueDetected: ((CueStats) -> Unit)? = null
    
    fun setCueDetectionCallback(callback: (CueStats) -> Unit) {
        onCueDetected = callback
    }
    
    fun startScreenCapture(resultCode: Int, data: android.content.Intent) {
        try {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(displayMetrics)
            
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            val density = displayMetrics.densityDpi
            
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            
            mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                Handler(Looper.getMainLooper())
            )
            
            isCapturing = true
            startPeriodicDetection()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun startPeriodicDetection() {
        val handler = Handler(Looper.getMainLooper())
        val detectionInterval = 2000L // Check every 2 seconds
        
        val detectionRunnable = object : Runnable {
            override fun run() {
                if (isCapturing) {
                    captureAndDetectCue()
                    handler.postDelayed(this, detectionInterval)
                }
            }
        }
        
        handler.post(detectionRunnable)
    }
    
    private fun captureAndDetectCue() {
        try {
            val image = imageReader?.acquireLatestImage() ?: return
            
            val bitmap = imageToBitmap(image)
            image.close()
            
            if (bitmap != null) {
                detectCueFromImage(bitmap)
                bitmap.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun imageToBitmap(image: Image): Bitmap? {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Crop to center area where cue info is typically shown
            val cropWidth = bitmap.width / 2
            val cropHeight = bitmap.height / 4
            val x = (bitmap.width - cropWidth) / 2
            val y = bitmap.height / 8
            
            return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun detectCueFromImage(bitmap: Bitmap) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            
            textRecognizer.process(image)
                .addOnSuccessListener { result ->
                    val detectedText = result.text
                    
                    // Try to match detected text with cue database
                    for ((cueName, cueStats) in cueDatabase) {
                        if (detectedText.contains(cueName, ignoreCase = true)) {
                            if (currentCue.id != cueStats.id) {
                                currentCue = cueStats
                                onCueDetected?.invoke(cueStats)
                            }
                            break
                        }
                    }
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                }
                
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun getCurrentCue(): CueStats = currentCue
    
    fun stopScreenCapture() {
        isCapturing = false
        mediaProjection?.stop()
        imageReader?.close()
        mediaProjection = null
        imageReader = null
    }
    
    fun getAllCues(): List<CueStats> = cueDatabase.values.toList()
    
    fun getCueByName(name: String): CueStats? {
        return cueDatabase.values.find { it.name.equals(name, ignoreCase = true) }
    }
}