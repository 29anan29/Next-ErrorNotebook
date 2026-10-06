package com.errorbook.app.domain.usecase

import com.errorbook.app.data.local.entity.QuestionEntity
import com.errorbook.app.data.repository.QuestionRepository
import javax.inject.Inject

class SaveQuestionUseCase @Inject constructor(
    private val questionRepository: QuestionRepository,
) {
    suspend operator fun invoke(imagePath: String, ocrText: String?, note: String?, source: String?, subjectId: Long?, reasonLinks: List<Pair<Long, String?>>): Long {
        require(reasonLinks.isNotEmpty()) { "至少需要一个错因" }
        val question = QuestionEntity(
            subjectId = subjectId,
            imagePath = imagePath,
            ocrText = ocrText?.takeIf { it.isNotBlank() },
            note = note?.takeIf { it.isNotBlank() },
            source = source?.takeIf { it.isNotBlank() },
            saveDirUri = null,
            createdAt = System.currentTimeMillis(),
        )
        return questionRepository.saveQuestionWithReasons(question, reasonLinks)
    }
}
