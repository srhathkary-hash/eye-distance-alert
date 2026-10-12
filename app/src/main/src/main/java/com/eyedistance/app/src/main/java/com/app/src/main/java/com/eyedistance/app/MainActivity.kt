package com.eyedistance.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var statusText: TextView
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // واجهة برمجية بحتة لا تحتاج أي ملف XML أو ثيم
        statusText = TextView(this).apply {
            text = "جاري التحقق من المسافة..."
            textSize = 22f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        setContentView(statusText)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.VIBRATE), 101
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, FaceDistanceAnalyzer(
                            onTooCloseTriggered = {
                                runOnUiThread {
                                    statusText.text = "⚠️ تحذير: الهاتف قريب جداً من عينيك!"
                                    statusText.setBackgroundColor(Color.RED)
                                    triggerVibration()
                                }
                            },
                            onSafeDistance = {
                                runOnUiThread {
                                    statusText.text = "✅ المسافة آمنة وممتازة"
                                    statusText.setBackgroundColor(Color.rgb(0, 150, 0))
                                }
                            }
                        ))
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer)
            } catch (_: Exception) {
                runOnUiThread {
                    statusText.text = "تعذر فتح الكاميرا الأمامية"
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            statusText.text = "يرجى منح إذن الكاميرا ليعمل التنبيه"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
