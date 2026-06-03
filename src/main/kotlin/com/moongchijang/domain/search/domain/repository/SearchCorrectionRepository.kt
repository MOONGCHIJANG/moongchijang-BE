package com.moongchijang.domain.search.domain.repository

import com.moongchijang.domain.search.domain.SearchCorrection
import org.springframework.data.jpa.repository.JpaRepository

interface SearchCorrectionRepository : JpaRepository<SearchCorrection, Long> {
    fun findBySourceKeywordAndEnabledTrue(sourceKeyword: String): SearchCorrection?
}
