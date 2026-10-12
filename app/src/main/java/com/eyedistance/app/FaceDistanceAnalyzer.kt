package com.eyedistance.app

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDistanceAnalyzer(
    private val onTooCloseTriggered: () -> Unit,
    private val onSafeDistance: () -> Unit,
    private val onFaceLost: () -> Unit // بروتوكول جديد عند فقدان الوجه
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .build()
    private val detector = FaceDetection.getClient(options)
    private var isProcessing = false

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        isProcessing = true
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces.first()
                        // حساب نسبة الوجه من الشاشة ليعمل بدقة على أي هاتف
                        val faceHeightRatio = face.boundingBox.height().toFloat() / image.height.toFloat()
                        
                        if (faceHeightRatio > 0.35f) { // إذا غطى الوجه 35% من الكاميرا
                            onTooCloseTriggered()
                        } else {
                            onSafeDistance()
                        }
                    } else {
                        // الكاميرا لا ترى وجهاً (إما أن الهاتف ملتسق بالوجه أو المستخدم لا ينظر)
                        onFaceLost()
                    }
                }
                .addOnCompleteListener {
                    isProcessing = false
                    imageProxy.close()
                }
        } else {
            isProcessing = false
            imageProxy.close()
        }
    }
}
