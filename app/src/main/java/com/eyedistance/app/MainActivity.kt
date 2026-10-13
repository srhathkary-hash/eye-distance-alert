package com.eyedistance.app

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#121212"))
        }
        
        val testBtn = Button(this).apply {
            text = "اختبار رسالة الدين (واتساب)"
            textSize = 20f
            setBackgroundColor(android.graphics.Color.parseColor("#25D366")) // لون واتساب
            setTextColor(android.graphics.Color.WHITE)
            
            setOnClickListener {
                val testPhone = "07700000000" // ضع رقمك الحقيقي هنا للاختبار
                val testMsg = "مرحباً، تم تفعيل اشتراكك.\n⚠️ يرجى ملاحظة وجود دين متبقي: 15,000 دينار."
                WhatsAppEngine.sendAlert(this@MainActivity, testPhone, testMsg)
            }
        }

        layout.addView(testBtn)
        setContentView(layout)
    }
}
