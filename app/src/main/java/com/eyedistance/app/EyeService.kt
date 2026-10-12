package com.eyedistance.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors

class EyeService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private var blackScreenView: TextView? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var lastAnalyzedTimestamp = 0L
    private var missingFaceCount = 0

    override fun onCreate() {
        super.onCreate()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("EYE_ALERT", "المراقبة", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        startForeground(1, NotificationCompat.Builder(this, "EYE_ALERT")
            .setContentTitle("درع المسافة نشط")
            .setContentText("يتم فحص المسافة كل 5 ثوانٍ لحماية عينيك")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build())

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startBackgroundCamera()
    }

    private fun startBackgroundCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also {
                    it.setAnalyzer(cameraExecutor, { imageProxy ->
                        val currentTime = System.currentTimeMillis()
                        // خنق المعالجة: 5000 تعني التحليل مرة كل 5 ثوانٍ
                        if (currentTime - lastAnalyzedTimestamp >= 5000) {
                            lastAnalyzedTimestamp = currentTime
                            processImage(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    })
                }
            cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analyzer)
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImage(imageProxy: androidx.camera.core.ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val detector = FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).build())
        
        detector.process(image).addOnSuccessListener { faces ->
            if (faces.isNotEmpty()) {
                missingFaceCount = 0 // تصفير العداد عند رصد وجه سليم
                val faceRatio = faces.first().boundingBox.height().toFloat() / image.height.toFloat()
                if (faceRatio > 0.35f) {
                    showBlackScreen("أنت قريب جداً!\nأبعد الهاتف لترى الشاشة")
                } else {
                    hideBlackScreen()
                }
            } else {
                // معالجة "العمى الخوارزمي": إذا اختفى الوجه، نزيد العداد
                missingFaceCount++
                if (missingFaceCount >= 2) { 
                    // إذا استمر العمى لمرتين متتاليتين، نفترض أن الهاتف ملتصق بالوجه
                    showBlackScreen("لم يتم رصد وجه!\n(هل الهاتف ملتصق بوجهك؟)")
                }
            }
        }.addOnCompleteListener { imageProxy.close() }
    }

    private fun showBlackScreen(message: String) {
        if (blackScreenView != null) {
            blackScreenView?.text = message
            return
        }
        blackScreenView = TextView(this).apply {
            text = message
            textSize = 24f
            setTextColor(Color.RED)
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(blackScreenView, params)
    }

    private fun hideBlackScreen() {
        blackScreenView?.let {
            windowManager.removeView(it)
            blackScreenView = null
        }
    }
}
