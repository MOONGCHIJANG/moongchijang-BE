package com.moongchijang.domain.search.application

import com.moongchijang.domain.search.domain.repository.SearchCorrectionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class SearchCorrectionService(
    private val searchCorrectionRepository: SearchCorrectionRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun correct(query: String): String? {
        val normalized = SearchQueryNormalizer.normalize(query)
        if (normalized.isBlank()) {
            return null
        }

        val correction = searchCorrectionRepository.findBySourceKeywordAndEnabledTrue(normalized)
            ?: return null
        correction.recordHit()
        return correction.targetKeyword
    }
}
