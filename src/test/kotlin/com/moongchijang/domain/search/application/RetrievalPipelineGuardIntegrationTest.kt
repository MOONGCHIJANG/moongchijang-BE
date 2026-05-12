package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.search.application.dto.SearchCase
import com.moongchijang.domain.search.application.port.VectorSearchCandidate
import com.moongchijang.domain.search.application.port.VectorSearchPort
import com.moongchijang.domain.search.domain.SearchIntent
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.global.config.SearchProperties
import com.moongchijang.support.search.SearchTestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.time.LocalDateTime

class RetrievalPipelineGuardIntegrationTest {

    private val groupBuyRepository: GroupBuyRepository = Mockito.mock(GroupBuyRepository::class.java)
    private val vectorSearchPort: VectorSearchPort = Mockito.mock(VectorSearchPort::class.java)
    private val aliasDictionary = AliasDictionary()
    private val reranker = SearchReranker()

    // Kotlin null-safety 우회용 헬퍼: Mockito 매처를 등록한 뒤 non-null 값을 반환.
    private fun anyLocalDateTime(): LocalDateTime {
        ArgumentMatchers.any(LocalDateTime::class.java)
        return LocalDateTime.MIN
    }
    private fun anyGroupBuyStatus(): GroupBuyStatus {
        ArgumentMatchers.any(GroupBuyStatus::class.java)
        return GroupBuyStatus.IN_PROGRESS
    }
    private fun anyLongList(): List<Long> {
        ArgumentMatchers.anyList<Long>()
        return emptyList()
    }

    private val seoulMatch = SearchTestFixtures.groupBuy(
        id = 1L,
        productName = "두쫀쿠",
        store = SearchTestFixtures.store(region = RegionType.SEOUL)
    )

    private fun pipeline(guardEnabled: Boolean): RetrievalPipeline {
        val properties = SearchProperties(guard = SearchProperties.Guard(enabled = guardEnabled))
        return RetrievalPipeline(
            groupBuyRepository = groupBuyRepository,
            vectorSearchPort = vectorSearchPort,
            aliasDictionary = aliasDictionary,
            vectorCandidatePromotionGuard = VectorCandidatePromotionGuard(properties),
            reranker = reranker,
            searchObservabilityLogger = SearchObservabilityLogger(properties),
            searchProperties = properties
        )
    }

    private fun stubExactReturns(result: List<GroupBuy>) {
        Mockito.`when`(
            groupBuyRepository.searchByIntent(
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
                anyLocalDateTime(),
                anyGroupBuyStatus()
            )
        ).thenReturn(result)
    }

    private fun stubVectorReturns(candidates: List<VectorSearchCandidate>) {
        Mockito.`when`(
            vectorSearchPort.search(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyDouble()
            )
        ).thenReturn(candidates)
    }

    private fun stubFindAllById(result: List<GroupBuy>) {
        Mockito.`when`(groupBuyRepository.findAllById(anyLongList()))
            .thenReturn(result)
    }

    @Test
    fun `guard ON - PRODUCT_ONLY 인텐트에서 product 일치 후보는 정당하게 promote된다`() {
        val intent = SearchIntent(
            region = null,
            product = "두쫀쿠",
            searchCase = SearchCase.PRODUCT_ONLY,
            confidence = 0.7
        )
        stubExactReturns(emptyList())
        stubVectorReturns(listOf(VectorSearchCandidate(1L, 0.9)))
        stubFindAllById(listOf(seoulMatch))

        val results = pipeline(guardEnabled = true).retrieve("강남 두쫀쿠", intent)

        assertThat(results).hasSize(1)
        assertThat(results[0].groupBuy.id).isEqualTo(1L)
        assertThat(results[0].matchedBy).contains("VECTOR")
    }

    @Test
    fun `guard ON - 알 수 없는 인텐트(NONE_DETECTED)는 NO_KNOWN_TOKEN 사유로 벡터 후보를 차단한다`() {
        val intent = SearchIntent(
            region = null,
            product = null,
            searchCase = SearchCase.NONE_DETECTED,
            confidence = 0.0
        )
        stubExactReturns(emptyList())
        stubVectorReturns(listOf(VectorSearchCandidate(1L, 0.95)))
        stubFindAllById(listOf(seoulMatch))

        val results = pipeline(guardEnabled = true).retrieve("asdfqwer", intent)

        assertThat(results).isEmpty()
    }

    @Test
    fun `guard OFF - 동일 NONE_DETECTED 인풋에서는 false positive가 그대로 통과한다`() {
        val intent = SearchIntent(
            region = null,
            product = null,
            searchCase = SearchCase.NONE_DETECTED,
            confidence = 0.0
        )
        stubExactReturns(emptyList())
        stubVectorReturns(listOf(VectorSearchCandidate(1L, 0.95)))
        stubFindAllById(listOf(seoulMatch))

        val results = pipeline(guardEnabled = false).retrieve("asdfqwer", intent)

        // guard 비활성 시 벡터 후보가 결과로 노출되어 guard의 가치를 증명
        assertThat(results).hasSize(1)
        assertThat(results[0].groupBuy.id).isEqualTo(1L)
        assertThat(results[0].matchedBy).contains("VECTOR")
    }

    @Test
    fun `guard ON - 인텐트 product와 매장 productName 불일치 시 METADATA_MISMATCH로 차단한다`() {
        val intent = SearchIntent(
            region = null,
            product = "두쫀쿠",
            searchCase = SearchCase.PRODUCT_ONLY,
            confidence = 0.9
        )
        val mismatch = SearchTestFixtures.groupBuy(
            id = 2L,
            productName = "소금빵",
            store = SearchTestFixtures.store(region = RegionType.SEOUL)
        )
        stubExactReturns(emptyList())
        stubVectorReturns(listOf(VectorSearchCandidate(2L, 0.92)))
        stubFindAllById(listOf(mismatch))

        val results = pipeline(guardEnabled = true).retrieve("두쫀쿠", intent)

        assertThat(results).isEmpty()
    }
}
