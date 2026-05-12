package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.groupbuy.application.GroupBuyService
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyImage
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyImageRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class GroupBuyServiceTest {

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var groupBuyImageRepository: GroupBuyImageRepository

    @Mock
    private lateinit var favoriteRepository: FavoriteRepository

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @InjectMocks
    private lateinit var service: GroupBuyService

    @Test
    fun `공구 피드 조회 시 keyword 없이도 정상 조회된다`() {
        val groupBuy = createGroupBuy(id = 21L, status = GroupBuyStatus.IN_PROGRESS)
        val pageable = PageRequest.of(0, 20)
        val request = com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedRequest(
            filter = com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter.ALL,
            districts = emptyList()
        )

        `when`(
            groupBuyRepository.searchFeed(
                filter = request.filter,
                districtFilters = emptySet(),
                pageable = pageable
            )
        ).thenReturn(PageImpl(listOf(groupBuy), pageable, 1))

        val result = service.getFeed(request, pageable)

        assertEquals(1, result.content.size)
        assertEquals(21L, result.content.first().id)
        verify(groupBuyRepository).searchFeed(
            filter = request.filter,
            districtFilters = emptySet(),
            pageable = pageable
        )
    }

    @Test
    fun `로그인 사용자 상세 조회 시 찜 여부와 참여 여부 반환`() {
        val groupBuyId = 10L
        val userId = 1L
        val groupBuy = createGroupBuy(id = groupBuyId, status = GroupBuyStatus.IN_PROGRESS)
        val images = listOf(createImage(groupBuy, "https://image1.jpg"), createImage(groupBuy, "https://image2.jpg"))

        `when`(groupBuyRepository.findWithStoreById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(groupBuyImageRepository.findAllByGroupBuyId(groupBuyId)).thenReturn(images)
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(true)
        `when`(participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(true)

        val result = service.getDetail(groupBuyId, userId)

        assertEquals(groupBuyId, result.id)
        assertTrue(result.isWishlisted)
        assertTrue(result.isParticipated)
        assertFalse(result.canParticipate)
        assertEquals(listOf("https://image1.jpg", "https://image2.jpg"), result.imageUrls)
    }

    @Test
    fun `비로그인 사용자 상세 조회 시 찜 여부와 참여 여부 false 반환`() {
        val groupBuyId = 11L
        val groupBuy = createGroupBuy(id = groupBuyId, status = GroupBuyStatus.IN_PROGRESS)

        `when`(groupBuyRepository.findWithStoreById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(groupBuyImageRepository.findAllByGroupBuyId(groupBuyId)).thenReturn(emptyList())

        val result = service.getDetail(groupBuyId, null)

        assertFalse(result.isWishlisted)
        assertFalse(result.isParticipated)
        assertTrue(result.canParticipate)
    }

    @Test
    fun `마감된 공구 상세 조회 시 참여 가능 여부 false 반환`() {
        val groupBuyId = 12L
        val userId = 3L
        val groupBuy = createGroupBuy(id = groupBuyId, status = GroupBuyStatus.CLOSED)

        `when`(groupBuyRepository.findWithStoreById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(groupBuyImageRepository.findAllByGroupBuyId(groupBuyId)).thenReturn(emptyList())
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)
        `when`(participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)

        val result = service.getDetail(groupBuyId, userId)

        assertFalse(result.canParticipate)
    }

    @Test
    fun `존재하지 않는 공구 상세 조회 시 GROUPBUY_NOT_FOUND 예외 발생`() {
        `when`(groupBuyRepository.findWithStoreById(999L)).thenReturn(Optional.empty())

        val ex = assertThrows<CustomException> { service.getDetail(999L, 1L) }

        assertEquals(ErrorCode.GROUPBUY_NOT_FOUND, ex.errorCode)
    }

    private fun createGroupBuy(
        id: Long,
        status: GroupBuyStatus,
        deadline: LocalDateTime = LocalDateTime.now().plusDays(3)
    ): GroupBuy {
        val store = createStore()
        val request = createGroupBuyRequest()
        return GroupBuy(
            store = store,
            groupBuyRequest = request,
            thumbnailUrl = "https://example.jpg",
            productName = "두쫀쿠 1개",
            productDescription = "설명",
            price = 6000,
            targetQuantity = 50,
            currentQuantity = 36,
            maxQuantity = 100,
            status = status,
            deadline = deadline,
            pickupDate = LocalDate.now().plusDays(5),
            pickupTimeStart = LocalTime.of(14, 0),
            pickupTimeEnd = LocalTime.of(18, 0),
            pickupLocation = "서울 성동구 성수동",
            shareCount = 0
        ).apply { this.id = id }
    }

    private fun createStore(): Store =
        Store(
            name = "뭉치장 베이커리",
            address = "서울 성동구",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN
        ).apply {
            id = 1L
            latitude = 37.544
            longitude = 127.055
        }

    private fun createGroupBuyRequest(): GroupBuyRequest =
        GroupBuyRequest(
            userId = 1L,
            storeName = "뭉치장 베이커리",
            storeAddress = "서울 성동구",
            productName = "두쫀쿠 1개",
            desiredQuantity = 50,
            desiredPickupDate = LocalDate.now().plusDays(5)
        ).apply { id = 20L }

    private fun createImage(groupBuy: GroupBuy, imageUrl: String): GroupBuyImage =
        GroupBuyImage(
            groupBuy = groupBuy,
            imageUrl = imageUrl
        )
}
