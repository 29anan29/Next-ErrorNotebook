package com.errorbook.app.domain.usecase

import com.errorbook.app.data.model.ReasonWithCount
import com.errorbook.app.data.repository.ReasonRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetReasonRankingUseCase @Inject constructor(
    private val reasonRepository: ReasonRepository,
) {
    operator fun invoke(subjectId: Long?, since: Long?): Flow<List<ReasonWithCount>> = reasonRepository.getRanking(subjectId, since)
}
