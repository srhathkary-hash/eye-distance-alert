package com.eyedistance.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // طلب إذن الكاميرا أولاً
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        
        val startBtn = Button(this).apply {
            text = "تفعيل درع الحماية"
            textSize = 22f
            setBackgroundColor(android.graphics.Color.rgb(0, 150, 0))
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                    Toast.makeText(this@MainActivity, "يجب تفعيل 'الظهور فوق التطبيقات الأخرى' ليعمل الدرع", Toast.LENGTH_LONG).show()
                } else {
                    ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, EyeService::class.java))
                    Toast.makeText(this@MainActivity, "الدرع نشط في الخلفية الآن", Toast.LENGTH_SHORT).show()
                    finish() // إغلاق الواجهة وترك الدرع يعمل
                }
            }
        }

        val stopBtn = Button(this).apply {
            text = "إيقاف الدرع"
            textSize = 18f
            setBackgroundColor(android.graphics.Color.RED)
            setTextColor(android.graphics.Color.WHITE)
            setOnClickListener {
                stopService(Intent(this@MainActivity, EyeService::class.java))
            }
        }

        layout.addView(startBtn)
        layout.addView(stopBtn)
        setContentView(layout)
    }
}
