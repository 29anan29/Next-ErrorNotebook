package com.errorbook.app.platform.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * ML Kit 简体中文 OCR 封装。
 *
 * 使用的是 com.google.mlkit:text-recognition-chinese 内置模型：
 * 识别完全离线运行，首次使用不需要联网下载模型；这既是产品的
 * 「数据不出设备」约束，也是 AndroidManifest 剔除 INTERNET 的前提。
 */
@Singleton
class OcrService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    suspend fun recognize(imageUri: Uri): Result<String> = suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val cleaned = visionText.text
                        .lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString("\n")
                    cont.resume(Result.success(cleaned))
                }
                .addOnFailureListener { e ->
                    cont.resume(Result.failure(e))
                }
        } catch (e: Exception) {
            cont.resume(Result.failure(e))
        }
    }

    fun close() = recognizer.close()
}
