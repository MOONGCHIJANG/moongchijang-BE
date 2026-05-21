package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyOpenRequestService
import com.moongchijang.domain.groupbuy.application.dto.CreateGroupBuyOpenRequestRequest
import com.moongchijang.domain.groupbuy.application.dto.StoreRecommendationRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOpenRequest
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyOpenRequestRepository
import com.moongchijang.domain.store.domain.repository.StoreRepository
import com.moongchijang.domain.store.infrastructure.naver.NaverLocalSearchClient
import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchResponse
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.NaverFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException

@ExtendWith(MockitoExtension::class)
class GroupBuyOpenRequestServiceTest {

    @Mock
    private lateinit var openRequestRepository: GroupBuyOpenRequestRepository

    @Mock
    private lateinit var naverLocalSearchClient: NaverLocalSearchClient

    @Mock
    private lateinit var storeRepository: StoreRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @InjectMocks
    private lateinit var service: GroupBuyOpenRequestService

    @Test
    fun `정상 알림 신청 시 저장 성공`() {
        val userId = 1L
        val request = CreateGroupBuyOpenRequestRequest(region = "성수", productName = "소금빵")

        `when`(openRequestRepository.existsByUserIdAndRegionAndProductName(userId, "성수", "소금빵"))
            .thenReturn(false)
        `when`(openRequestRepository.saveAndFlush(any())).thenReturn(
            GroupBuyOpenRequest(userId = userId, region = "성수", productName = "소금빵").apply { id = 1L }
        )

        service.create(userId, request)

        verify(openRequestRepository).saveAndFlush(any())
    }

    @Test
    fun `중복 알림 신청 시 DUPLICATE_OPEN_REQUEST 예외`() {
        val userId = 1L
        val request = CreateGroupBuyOpenRequestRequest(region = "성수", productName = "소금빵")

        `when`(openRequestRepository.existsByUserIdAndRegionAndProductName(userId, "성수", "소금빵"))
            .thenReturn(true)

        val ex = assertThrows<CustomException> { service.create(userId, request) }
        assertEquals(ErrorCode.DUPLICATE_OPEN_REQUEST, ex.errorCode)
        verify(openRequestRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `동시 신청으로 UNIQUE 제약 위반 시 DUPLICATE_OPEN_REQUEST 예외`() {
        val userId = 1L
        val request = CreateGroupBuyOpenRequestRequest(region = "성수", productName = "소금빵")

        `when`(openRequestRepository.existsByUserIdAndRegionAndProductName(userId, "성수", "소금빵"))
            .thenReturn(false)
        `when`(openRequestRepository.saveAndFlush(any<GroupBuyOpenRequest>()))
            .thenThrow(DataIntegrityViolationException("uk_open_req_user_region_product"))

        val ex = assertThrows<CustomException> { service.create(userId, request) }
        assertEquals(ErrorCode.DUPLICATE_OPEN_REQUEST, ex.errorCode)
    }

    @Test
    fun `매장 추천 시 네이버 결과를 중복 제거하고 추천 점수 순으로 반환한다`() {
        val request = StoreRecommendationRequest(region = "성수", productName = "소금빵")
        val loaf = NaverFixture.naverItem(
            title = "<b>LOAF</b>",
            link = "https://map.naver.com/p/entry/place/100",
            category = "음식점>카페,디저트",
            address = "서울 성동구 성수동1가 1",
            roadAddress = "서울 성동구 성수이로 1"
        )
        val duplicateLoaf = loaf.copy(description = "duplicate")
        val other = NaverFixture.naverItem(
            title = "다른 가게",
            link = "https://map.naver.com/p/entry/place/200",
            category = "생활,편의",
            address = "서울 마포구 망원동 1",
            roadAddress = "서울 마포구 월드컵로 1"
        )
        val registeredStore = GroupBuyFixture.createStore(
            name = "LOAF",
            address = "서울 성동구 성수이로 1"
        ).apply { id = 10L }

        `when`(naverLocalSearchClient.search("성수 소금빵", 20)).thenReturn(
            NaverLocalSearchResponse(
                total = 3,
                start = 1,
                display = 3,
                items = listOf(other, loaf, duplicateLoaf)
            )
        )
        `when`(storeRepository.findByNormalizedNameIn(anyCollection())).thenReturn(listOf(registeredStore))
        `when`(storeRepository.findByNormalizedAddressIn(anyCollection())).thenReturn(emptyList())
        `when`(groupBuyRepository.findStoreIdsWithGroupBuyHistory(setOf(10L))).thenReturn(listOf(10L))

        val response = service.recommendStores(request)

        assertEquals("성수", response.region)
        assertEquals("소금빵", response.productName)
        assertEquals(2, response.stores.size)
        assertEquals("LOAF", response.stores[0].storeName)
        assertEquals("100", response.stores[0].placeId)
        assertTrue(response.stores[0].addressMatched)
        assertTrue(response.stores[0].categoryMatched)
        assertTrue(response.stores[0].registeredStore)
        assertTrue(response.stores[0].previousGroupBuyStore)
        assertEquals("다른 가게", response.stores[1].storeName)
        assertFalse(response.stores[1].registeredStore)
        verify(storeRepository).findByNormalizedNameIn(setOf("다른가게", "loaf"))
        verify(storeRepository).findByNormalizedAddressIn(
            setOf(
                "서울마포구월드컵로1",
                "서울마포구망원동1",
                "서울성동구성수이로1",
                "서울성동구성수동1가1"
            )
        )
    }

    @Test
    fun `네이버 API 실패 시 빈 추천 목록으로 fallback 한다`() {
        val request = StoreRecommendationRequest(region = "성수", productName = "소금빵")
        `when`(naverLocalSearchClient.search("성수 소금빵", 20))
            .thenThrow(RuntimeException("timeout"))

        val response = service.recommendStores(request)

        assertEquals("성수", response.region)
        assertEquals("소금빵", response.productName)
        assertTrue(response.stores.isEmpty())
        verifyNoInteractions(storeRepository, groupBuyRepository)
    }

    @Test
    fun `네이버 결과가 0건이면 빈 추천 목록을 반환한다`() {
        val request = StoreRecommendationRequest(region = "성수", productName = "소금빵")
        `when`(naverLocalSearchClient.search("성수 소금빵", 20)).thenReturn(
            NaverLocalSearchResponse(total = 0, start = 1, display = 0, items = emptyList())
        )

        val response = service.recommendStores(request)

        assertTrue(response.stores.isEmpty())
        verifyNoInteractions(storeRepository, groupBuyRepository)
    }
}
