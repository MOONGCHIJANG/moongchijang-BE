package com.moongchijang.domain.search.application

import com.moongchijang.domain.search.domain.SearchCorrection
import com.moongchijang.domain.search.domain.SearchCorrectionType
import com.moongchijang.domain.search.domain.repository.SearchCorrectionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class SearchCorrectionServiceTest {
    private val searchCorrectionRepository: SearchCorrectionRepository = Mockito.mock(SearchCorrectionRepository::class.java)
    private val service = SearchCorrectionService(searchCorrectionRepository)

    @Test
    @DisplayName("검색어를 정규화한 뒤 보정 사전을 조회하고 hit count를 증가시킨다")
    fun `correct normalizes source keyword and records hit`() {
        val correction = SearchCorrection(
            sourceKeyword = "카래",
            targetKeyword = "카레",
            type = SearchCorrectionType.TYPO,
        )
        Mockito.`when`(searchCorrectionRepository.findBySourceKeywordAndEnabledTrue("카래"))
            .thenReturn(correction)

        val corrected = service.correct(" 카-래 ")

        assertThat(corrected).isEqualTo("카레")
        assertThat(correction.hitCount).isEqualTo(1)
    }

    @Test
    @DisplayName("정규화 후 검색어가 비어 있으면 사전을 조회하지 않는다")
    fun `blank normalized query skips repository`() {
        val corrected = service.correct(" - _ ")

        assertThat(corrected).isNull()
        Mockito.verifyNoInteractions(searchCorrectionRepository)
    }
}
