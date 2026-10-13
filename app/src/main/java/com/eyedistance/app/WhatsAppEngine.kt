package com.eyedistance.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WhatsAppEngine {
    fun sendAlert(context: Context, phone: String, message: String) {
        try {
            val formattedPhone = if (phone.startsWith("0")) "964${phone.substring(1)}" else phone
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage")
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "لم يتم العثور على واتساب", Toast.LENGTH_SHORT).show()
        }
    }
}
