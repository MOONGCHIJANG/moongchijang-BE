package com.moongchijang.domain.search.application

import com.moongchijang.domain.search.application.port.VectorSearchCandidate
import com.moongchijang.domain.search.domain.SearchIntent
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.global.config.SearchProperties
import com.moongchijang.support.search.SearchTestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class VectorCandidatePromotionGuardTest {

    private val defaultProperties = SearchProperties()
    private val guard = VectorCandidatePromotionGuard(defaultProperties)

    @Test
    fun `guard 비활성화 시 모든 후보를 통과시킨다`() {
        val disabledGuard = VectorCandidatePromotionGuard(
            SearchProperties(guard = SearchProperties.Guard(enabled = false))
        )
        val intent = SearchIntent(region = null, product = null, searchCase = SearchCase.NONE_DETECTED, confidence = 0.0)
        val groupBuy = SearchTestFixtures.groupBuy(id = 1L, productName = "두쫀쿠")
        val candidates = listOf(VectorSearchCandidate(1L, 0.3))

        val result = disabledGuard.filterPromotableCandidates(intent, listOf(groupBuy), candidates)

        assertThat(result.vectorCandidates).hasSize(1)
        assertThat(result.decisions.first().rejectionReason).isNull()
    }

    @Test
    fun `known token이 없으면 NO_KNOWN_TOKEN 사유로 거부한다`() {
        val intent = SearchIntent(region = null, product = null, searchCase = SearchCase.NONE_DETECTED, confidence = 0.9)
        val groupBuy = SearchTestFixtures.groupBuy(id = 1L)
        val candidates = listOf(VectorSearchCandidate(1L, 0.9))

        val result = guard.filterPromotableCandidates(intent, listOf(groupBuy), candidates)

        assertThat(result.vectorCandidates).isEmpty()
        assertThat(result.decisions.first().rejectionReason)
            .isEqualTo(VectorCandidateRejectionReason.NO_KNOWN_TOKEN)
    }

    @Test
    fun `신뢰도가 임계치 미만이면 LOW_CONFIDENCE 사유로 거부한다`() {
        val intent = SearchIntent(region = "서울", product = "두쫀쿠", searchCase = SearchCase.BOTH_DETECTED, confidence = 0.5)
        val groupBuy = SearchTestFixtures.groupBuy(id = 1L, productName = "두쫀쿠")
        val candidates = listOf(VectorSearchCandidate(1L, 0.9))

        val result = guard.filterPromotableCandidates(intent, listOf(groupBuy), candidates)

        assertThat(result.decisions.first().rejectionReason)
            .isEqualTo(VectorCandidateRejectionReason.LOW_CONFIDENCE)
    }

    @Test
    fun `메타데이터(지역 또는 상품)가 불일치하면 METADATA_MISMATCH로 거부한다`() {
        val intent = SearchIntent(region = "서울", product = "소금빵", searchCase = SearchCase.BOTH_DETECTED, confidence = 0.9)
        val groupBuy = SearchTestFixtures.groupBuy(id = 1L, productName = "두쫀쿠")
        val candidates = listOf(VectorSearchCandidate(1L, 0.9))

        val result = guard.filterPromotableCandidates(intent, listOf(groupBuy), candidates)

        assertThat(result.decisions.first().rejectionReason)
            .isEqualTo(VectorCandidateRejectionReason.METADATA_MISMATCH)
    }

    @Test
    fun `vectorMatches에 메타데이터가 없으면 UNAVAILABLE_METADATA로 거부한다`() {
        val intent = SearchIntent(region = "서울", product = "두쫀쿠", searchCase = SearchCase.BOTH_DETECTED, confidence = 0.9)
        val candidates = listOf(VectorSearchCandidate(42L, 0.9))

        val result = guard.filterPromotableCandidates(intent, emptyList(), candidates)

        assertThat(result.decisions.first().rejectionReason)
            .isEqualTo(VectorCandidateRejectionReason.UNAVAILABLE_METADATA)
    }

    @Test
    fun `region만 있고 메타데이터가 일치하면 통과시킨다`() {
        val intent = SearchIntent(region = "서울", product = null, searchCase = SearchCase.NEIGHBORHOOD_ONLY, confidence = 0.7)
        val groupBuy = SearchTestFixtures.groupBuy(
            id = 1L,
            productName = "두쫀쿠",
            store = SearchTestFixtures.store(region = RegionType.SEOUL)
        )
        val candidates = listOf(VectorSearchCandidate(1L, 0.9))

        val result = guard.filterPromotableCandidates(intent, listOf(groupBuy), candidates)

        assertThat(result.vectorCandidates).hasSize(1)
        assertThat(result.decisions.first().rejectionReason).isNull()
    }

    @Test
    fun `product만 있고 일치할 때 통과시킨다`() {
        val intent = SearchIntent(region = null, product = "두쫀쿠", searchCase = SearchCase.PRODUCT_ONLY, confidence = 0.7)
        val groupBuy = SearchTestFixtures.groupBuy(id = 1L, productName = "두쫀쿠")
        val candidates = listOf(VectorSearchCandidate(1L, 0.9))

        val result = guard.filterPromotableCandidates(intent, listOf(groupBuy), candidates)

        assertThat(result.vectorCandidates).hasSize(1)
    }

    @Test
    fun `여러 후보 중 통과·거부 사유별 카운트가 정확하다`() {
        val intent = SearchIntent(region = "서울", product = "두쫀쿠", searchCase = SearchCase.BOTH_DETECTED, confidence = 0.9)
        val matching = SearchTestFixtures.groupBuy(id = 1L, productName = "두쫀쿠")
        val mismatch = SearchTestFixtures.groupBuy(id = 2L, productName = "소금빵")
        val candidates = listOf(
            VectorSearchCandidate(1L, 0.95),
            VectorSearchCandidate(2L, 0.9),
            VectorSearchCandidate(999L, 0.9)
        )

        val result = guard.filterPromotableCandidates(intent, listOf(matching, mismatch), candidates)

        assertThat(result.vectorCandidates.map { it.groupBuyId }).containsExactly(1L)
        assertThat(result.rejectedCount).isEqualTo(2)
        assertThat(result.rejectionReasonCounts)
            .containsEntry(VectorCandidateRejectionReason.METADATA_MISMATCH, 1)
            .containsEntry(VectorCandidateRejectionReason.UNAVAILABLE_METADATA, 1)
    }
}
