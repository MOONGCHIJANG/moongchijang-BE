package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.search.domain.SearchCorrection
import com.moongchijang.domain.search.domain.SearchCorrectionType
import com.moongchijang.domain.search.domain.repository.SearchCorrectionRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class SearchCorrectionServiceTest {
    private val searchCorrectionRepository: SearchCorrectionRepository = Mockito.mock(SearchCorrectionRepository::class.java)
    private val groupBuyRepository: GroupBuyRepository = Mockito.mock(GroupBuyRepository::class.java)
    private val service = SearchCorrectionService(searchCorrectionRepository, groupBuyRepository)

    @Test
    @DisplayName("검색어를 정규화한 뒤 보정 사전을 조회하고 target keyword를 반환한다")
    fun `correct normalizes source keyword and returns target keyword`() {
        val correction = SearchCorrection(
            sourceKeyword = "카래",
            targetKeyword = "카레",
            type = SearchCorrectionType.TYPO,
        )
        Mockito.`when`(searchCorrectionRepository.findBySourceKeywordAndEnabledTrue("카래"))
            .thenReturn(correction)

        val corrected = service.correct(" 카-래 ")

        assertThat(corrected).isEqualTo("카레")
        assertThat(correction.hitCount).isZero
    }

    @Test
    @DisplayName("정규화 후 검색어가 비어 있으면 사전을 조회하지 않는다")
    fun `blank normalized query skips repository`() {
        val corrected = service.correct(" - _ ")

        assertThat(corrected).isNull()
        Mockito.verifyNoInteractions(searchCorrectionRepository)
        Mockito.verifyNoInteractions(groupBuyRepository)
    }

    @Test
    @DisplayName("사전 source가 없어도 target keyword와 자모 유사도가 가까우면 보정한다")
    fun `corrects by jamo similarity against dictionary targets`() {
        val correction = SearchCorrection(
            sourceKeyword = "카래",
            targetKeyword = "카레",
            type = SearchCorrectionType.TYPO,
        )
        Mockito.`when`(searchCorrectionRepository.findBySourceKeywordAndEnabledTrue("카례"))
            .thenReturn(null)
        Mockito.`when`(searchCorrectionRepository.findAllByEnabledTrue())
            .thenReturn(listOf(correction))
        Mockito.`when`(groupBuyRepository.findActiveSearchKeywords("IN_PROGRESS", 500))
            .thenReturn(emptyList())

        val corrected = service.correct("카례")

        assertThat(corrected).isEqualTo("카레")
    }

    @Test
    @DisplayName("현재 피드 검색 후보 일부와 자모 유사도가 가까우면 해당 부분 검색어로 보정한다")
    fun `corrects by jamo similarity against active feed terms`() {
        Mockito.`when`(searchCorrectionRepository.findBySourceKeywordAndEnabledTrue("카례"))
            .thenReturn(null)
        Mockito.`when`(searchCorrectionRepository.findAllByEnabledTrue())
            .thenReturn(emptyList())
        Mockito.`when`(groupBuyRepository.findActiveSearchKeywords("IN_PROGRESS", 500))
            .thenReturn(listOf("카레라면"))

        val corrected = service.correct("카례")

        assertThat(corrected).isEqualTo("카레")
    }

    @Test
    @DisplayName("현재 피드의 매장명과 자모 유사도가 가까우면 매장명으로 보정한다")
    fun `corrects typo against active store name`() {
        Mockito.`when`(searchCorrectionRepository.findBySourceKeywordAndEnabledTrue("앙즈간루"))
            .thenReturn(null)
        Mockito.`when`(searchCorrectionRepository.findAllByEnabledTrue())
            .thenReturn(emptyList())
        Mockito.`when`(groupBuyRepository.findActiveSearchKeywords("IN_PROGRESS", 500))
            .thenReturn(listOf("양즈간루"))

        val corrected = service.correct("앙즈간루")

        assertThat(corrected).isEqualTo("양즈간루")
    }
}
