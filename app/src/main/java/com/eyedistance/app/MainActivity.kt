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
        val prefs = getSharedPreferences("CrashLog", Context.MODE_PRIVATE)
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            prefs.edit().putString("last_error", e.stackTraceToString()).commit()
            Runtime.getRuntime().exit(1)
        }

        statusText = TextView(this).apply {
            text = "جاري التحقق من المسافة..."
            textSize = 22f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        setContentView(statusText)

        val lastError = prefs.getString("last_error", null)
        if (lastError != null) {
            statusText.text = "جذر الخلل:\n\n$lastError"
            statusText.setTextColor(Color.RED)
            statusText.textSize = 11f
            prefs.edit().clear().apply()
            return
        }

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
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also {
                        it.setAnalyzer(cameraExecutor, FaceDistanceAnalyzer(
                            onTooCloseTriggered = { runOnUiThread { alertUser() } },
                            onSafeDistance = { runOnUiThread { safeUser() } },
                            onFaceLost = { runOnUiThread { lostUser() } } // تفعيل الحالة الجديدة
                        ))
                    }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, imageAnalyzer)
            } catch (e: Exception) {
                runOnUiThread { statusText.text = "فشل بدء الكاميرا:\n${e.message}" }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun alertUser() {
        statusText.text = "⚠️ تحذير: الهاتف قريب جداً!"
        statusText.setBackgroundColor(Color.RED)
        triggerVib()
    }

    private fun safeUser() {
        statusText.text = "✅ المسافة آمنة"
        statusText.setBackgroundColor(Color.rgb(0, 150, 0))
    }

    private fun lostUser() {
        statusText.text = "🔍 لم يتم رصد وجه\n(هل الهاتف ملاصق لوجهك أو الإضاءة معدومة؟)"
        statusText.setBackgroundColor(Color.DKGRAY)
    }

    private fun triggerVib() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    
    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, gr: IntArray) {
        super.onRequestPermissionsResult(rc, p, gr)
        if (rc == 101 && gr.isNotEmpty() && gr[0] == PackageManager.PERMISSION_GRANTED) startCamera()
        else statusText.text = "يرجى منح إذن الكاميرا"
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
