package com.errorbook.app.domain.usecase

import android.net.Uri
import com.errorbook.app.platform.ocr.OcrService
import javax.inject.Inject

class RecognizeTextUseCase @Inject constructor(
    private val ocrService: OcrService,
) {
    suspend operator fun invoke(imageUri: Uri): Result<String> = ocrService.recognize(imageUri)
}