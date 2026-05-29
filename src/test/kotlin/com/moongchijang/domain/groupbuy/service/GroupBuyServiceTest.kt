package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.groupbuy.application.GroupBuyService
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.FeedSortMode
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyImageRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.lenient
import org.mockito.Mockito.times
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

    @Mock
    private lateinit var s3ImageReferenceResolver: S3ImageReferenceResolver

    private lateinit var service: GroupBuyService

    @BeforeEach
    fun setUp() {
        lenient().`when`(s3ImageReferenceResolver.resolveForRead(anyString())).thenAnswer { it.arguments[0] as String? }
        service = GroupBuyService(
            groupBuyRepository = groupBuyRepository,
            groupBuyImageRepository = groupBuyImageRepository,
            favoriteRepository = favoriteRepository,
            participationRepository = participationRepository,
            s3ImageReferenceResolver = s3ImageReferenceResolver,
            shareBaseUrl = "https://moongchijang.com"
        )
    }

    @Test
    fun `공구 피드 keyword 제외 조회 검증`() {
        val groupBuy = GroupBuyFixture.createGroupBuy(id = 21L, status = GroupBuyStatus.IN_PROGRESS)
        val pageable = PageRequest.of(0, 20)
        val request = com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedRequest(
            filter = com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter.ALL,
            districts = emptyList()
        )

        `when`(
            groupBuyRepository.searchFeed(
                filter = request.filter,
                districtFilters = emptySet(),
                pageable = pageable,
                sortMode = FeedSortMode.REGIONAL
            )
        ).thenReturn(PageImpl(listOf(groupBuy), pageable, 1))

        val result = service.getFeed(request, pageable)

        assertEquals(1, result.content.size)
        assertEquals(21L, result.content.first().id)
        verify(groupBuyRepository).searchFeed(
            filter = request.filter,
            districtFilters = emptySet(),
            pageable = pageable,
            sortMode = FeedSortMode.REGIONAL
        )
    }

    @Test
    fun `지역 결과 없음 fallback 재조회 및 hasRegionalResult false 검증`() {
        val regionalDistrict = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG
        val pageable = PageRequest.of(0, 20)
        val request = com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedRequest(
            filter = com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter.ALL,
            districts = listOf(regionalDistrict)
        )
        val fallbackGroupBuy = GroupBuyFixture.createGroupBuy(id = 31L, status = GroupBuyStatus.IN_PROGRESS)

        `when`(
            groupBuyRepository.searchFeed(
                filter = request.filter,
                districtFilters = setOf(regionalDistrict),
                pageable = pageable,
                sortMode = FeedSortMode.REGIONAL
            )
        ).thenReturn(PageImpl(emptyList(), pageable, 0))

        `when`(
            groupBuyRepository.searchFeed(
                filter = request.filter,
                districtFilters = emptySet(),
                pageable = pageable,
                sortMode = FeedSortMode.NATIONWIDE_FALLBACK
            )
        ).thenReturn(PageImpl(listOf(fallbackGroupBuy), pageable, 1))

        val result = service.getFeed(request, pageable)

        assertFalse(result.hasRegionalResult)
        assertEquals(1, result.content.size)
        assertEquals(31L, result.content.first().id)
        verify(groupBuyRepository, times(1)).searchFeed(
            filter = request.filter,
            districtFilters = setOf(regionalDistrict),
            pageable = pageable,
            sortMode = FeedSortMode.REGIONAL
        )
        verify(groupBuyRepository, times(1)).searchFeed(
            filter = request.filter,
            districtFilters = emptySet(),
            pageable = pageable,
            sortMode = FeedSortMode.NATIONWIDE_FALLBACK
        )
    }

    @Test
    fun `지역 미설정 시 fallback 미수행 및 hasRegionalResult true 유지 검증`() {
        val pageable = PageRequest.of(0, 20)
        val request = com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedRequest(
            filter = com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedFilter.ALL,
            districts = emptyList()
        )

        `when`(
            groupBuyRepository.searchFeed(
                filter = request.filter,
                districtFilters = emptySet(),
                pageable = pageable,
                sortMode = FeedSortMode.REGIONAL
            )
        ).thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = service.getFeed(request, pageable)

        assertTrue(result.hasRegionalResult)
        assertEquals(0, result.content.size)
        verify(groupBuyRepository, times(1)).searchFeed(
            filter = request.filter,
            districtFilters = emptySet(),
            pageable = pageable,
            sortMode = FeedSortMode.REGIONAL
        )
        verify(groupBuyRepository, never()).searchFeed(
            filter = request.filter,
            districtFilters = emptySet(),
            pageable = pageable,
            sortMode = FeedSortMode.NATIONWIDE_FALLBACK
        )
    }

    @Test
    fun `로그인 사용자 상세 조회 시 찜 여부와 참여 여부 반환`() {
        val groupBuyId = 10L
        val userId = 1L
        val groupBuy = GroupBuyFixture.createGroupBuy(id = groupBuyId, status = GroupBuyStatus.IN_PROGRESS)
        val images = listOf(GroupBuyFixture.createImage(groupBuy, "https://image1.jpg"), GroupBuyFixture.createImage(groupBuy, "https://image2.jpg"))

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
    fun `상세 조회 시 썸네일과 동일한 키의 이미지는 imageUrls 에서 제외된다`() {
        val groupBuyId = 13L
        val userId = 1L
        val groupBuy = GroupBuyFixture.createGroupBuy(id = groupBuyId, status = GroupBuyStatus.IN_PROGRESS)
        val images = listOf(
            GroupBuyFixture.createImage(groupBuy, groupBuy.thumbnailKey!!),
            GroupBuyFixture.createImage(groupBuy, "https://image-detail.jpg"),
        )

        `when`(groupBuyRepository.findWithStoreById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(groupBuyImageRepository.findAllByGroupBuyId(groupBuyId)).thenReturn(images)
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)
        `when`(participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)

        val result = service.getDetail(groupBuyId, userId)

        assertEquals(listOf("https://image-detail.jpg"), result.imageUrls)
        assertEquals(groupBuy.thumbnailKey, result.thumbnailUrl)
    }

    @Test
    fun `비로그인 사용자 상세 조회 시 찜 여부와 참여 여부 false 반환`() {
        val groupBuyId = 11L
        val groupBuy = GroupBuyFixture.createGroupBuy(id = groupBuyId, status = GroupBuyStatus.IN_PROGRESS)

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
        val groupBuy = GroupBuyFixture.createGroupBuy(id = groupBuyId, status = GroupBuyStatus.CLOSED)

        `when`(groupBuyRepository.findWithStoreById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(groupBuyImageRepository.findAllByGroupBuyId(groupBuyId)).thenReturn(emptyList())
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)
        `when`(participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)

        val result = service.getDetail(groupBuyId, userId)

        assertFalse(result.canParticipate)
    }

    @Test
    fun `deadline 지난 IN_PROGRESS 상태 공구 상세 조회 시 참여 가능 여부 false 반환`() {
        val groupBuyId = 13L
        val userId = 4L
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = groupBuyId,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = LocalDateTime.now().minusMinutes(1)
        )

        `when`(groupBuyRepository.findWithStoreById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(groupBuyImageRepository.findAllByGroupBuyId(groupBuyId)).thenReturn(emptyList())
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)
        `when`(participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)

        val result = service.getDetail(groupBuyId, userId)

        assertFalse(result.canParticipate)
    }

    @Test
    fun `달성 완료 공구 상세 조회 시 최대 수량 전이면 참여 가능 여부 true 반환`() {
        val groupBuyId = 14L
        val userId = 5L
        val groupBuy = GroupBuyFixture.createGroupBuy(id = groupBuyId, status = GroupBuyStatus.ACHIEVED).apply {
            currentQuantity = 50
            maxQuantity = 100
        }

        `when`(groupBuyRepository.findWithStoreById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(groupBuyImageRepository.findAllByGroupBuyId(groupBuyId)).thenReturn(emptyList())
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)
        `when`(participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)

        val result = service.getDetail(groupBuyId, userId)

        assertFalse(result.isClosed)
        assertTrue(result.canParticipate)
    }

    @Test
    fun `달성 완료 공구 상세 조회 시 최대 수량 도달이면 참여 가능 여부 false 반환`() {
        val groupBuyId = 15L
        val userId = 6L
        val groupBuy = GroupBuyFixture.createGroupBuy(id = groupBuyId, status = GroupBuyStatus.ACHIEVED).apply {
            currentQuantity = 100
            maxQuantity = 100
        }

        `when`(groupBuyRepository.findWithStoreById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(groupBuyImageRepository.findAllByGroupBuyId(groupBuyId)).thenReturn(emptyList())
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)
        `when`(participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)

        val result = service.getDetail(groupBuyId, userId)

        assertFalse(result.isClosed)
        assertFalse(result.canParticipate)
    }

    @Test
    fun `deadline 지난 ACHIEVED 상태 공구 상세 조회 시 참여 가능 여부 false 반환`() {
        val groupBuyId = 16L
        val userId = 7L
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = groupBuyId,
            status = GroupBuyStatus.ACHIEVED,
            deadline = LocalDateTime.now().minusMinutes(1)
        ).apply {
            currentQuantity = 50
            maxQuantity = 100
        }

        `when`(groupBuyRepository.findWithStoreById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(groupBuyImageRepository.findAllByGroupBuyId(groupBuyId)).thenReturn(emptyList())
        `when`(favoriteRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)
        `when`(participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)

        val result = service.getDetail(groupBuyId, userId)

        assertFalse(result.isClosed)
        assertFalse(result.canParticipate)
    }

    @Test
    fun `존재하지 않는 공구 상세 조회 시 GROUPBUY_NOT_FOUND 예외 발생`() {
        `when`(groupBuyRepository.findWithStoreById(999L)).thenReturn(Optional.empty())

        val ex = assertThrows<CustomException> { service.getDetail(999L, 1L) }

        assertEquals(ErrorCode.GROUPBUY_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `공구 공유 메타데이터 조회 성공`() {
        val groupBuyId = 101L
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = groupBuyId,
            status = GroupBuyStatus.IN_PROGRESS,
            productName = "두쭌쿠 오리지널 1개"
        ).apply {
            thumbnailKey = "https://cdn.moongchijang.com/group-buys/101/thumbnail.jpg"
            productDescription = "지금 함께 주문하고 픽업해요."
            pickupDate = LocalDate.of(2026, 5, 15)
            pickupTimeStart = LocalTime.of(14, 0)
            pickupTimeEnd = LocalTime.of(18, 0)
        }

        `when`(groupBuyRepository.findWithStoreById(groupBuyId)).thenReturn(Optional.of(groupBuy))

        val result = service.getShareMeta(groupBuyId)

        assertEquals("https://moongchijang.com/group-buys/101", result.shareUrl)
        assertEquals("두쭌쿠 오리지널 1개", result.title)
        assertEquals("지금 함께 주문하고 픽업해요.", result.description)
        assertEquals("https://cdn.moongchijang.com/group-buys/101/thumbnail.jpg", result.imageUrl)
        assertEquals(groupBuy.store.name, result.storeName)
        assertEquals(groupBuy.deadline, result.deadline)
        assertEquals(groupBuy.pickupDate, result.pickupDate)
        assertEquals(groupBuy.pickupTimeStart, result.pickupTimeStart)
        assertEquals(groupBuy.pickupTimeEnd, result.pickupTimeEnd)
    }

    @Test
    fun `존재하지 않는 공구 공유 메타데이터 조회 시 GROUPBUY_NOT_FOUND 예외 발생`() {
        `when`(groupBuyRepository.findWithStoreById(999L)).thenReturn(Optional.empty())

        val ex = assertThrows<CustomException> { service.getShareMeta(999L) }

        assertEquals(ErrorCode.GROUPBUY_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `단건 progress 조회 성공`() {
        val groupBuyId = 21L
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = groupBuyId,
            status = GroupBuyStatus.IN_PROGRESS
        ).apply {
            currentQuantity = 36
            targetQuantity = 50
        }
        `when`(groupBuyRepository.findById(groupBuyId)).thenReturn(Optional.of(groupBuy))

        val result = service.getProgress(groupBuyId)

        assertEquals(groupBuyId, result.groupBuyId)
        assertEquals(72, result.achievementRate)
        assertEquals(36, result.currentQuantity)
        assertEquals(50, result.targetQuantity)
        assertFalse(result.isClosed)
    }

    @Test
    fun `달성 완료 공구 progress 조회 시 마감 여부 false 반환`() {
        val groupBuyId = 22L
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = groupBuyId,
            status = GroupBuyStatus.ACHIEVED
        ).apply {
            currentQuantity = 50
            targetQuantity = 50
        }
        `when`(groupBuyRepository.findById(groupBuyId)).thenReturn(Optional.of(groupBuy))

        val result = service.getProgress(groupBuyId)

        assertEquals(groupBuyId, result.groupBuyId)
        assertFalse(result.isClosed)
    }

    @Test
    fun `존재하지 않는 공구 progress 단건 조회 시 GROUPBUY_NOT_FOUND 예외 발생`() {
        `when`(groupBuyRepository.findById(999L)).thenReturn(Optional.empty())

        val ex = assertThrows<CustomException> { service.getProgress(999L) }

        assertEquals(ErrorCode.GROUPBUY_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `다건 progress 조회 시 요청 순서를 유지하고 존재하지 않는 id 는 제외한다`() {
        val first = GroupBuyFixture.createGroupBuy(id = 101L, status = GroupBuyStatus.IN_PROGRESS).apply {
            currentQuantity = 20
            targetQuantity = 50
        }
        val second = GroupBuyFixture.createGroupBuy(id = 202L, status = GroupBuyStatus.IN_PROGRESS).apply {
            currentQuantity = 45
            targetQuantity = 50
        }
        val requestIds = listOf(202L, 999L, 101L)

        `when`(groupBuyRepository.findAllById(requestIds)).thenReturn(listOf(first, second))

        val result = service.getProgresses(requestIds)

        assertEquals(2, result.size)
        assertEquals(202L, result[0].groupBuyId)
        assertEquals(90, result[0].achievementRate)
        assertEquals(101L, result[1].groupBuyId)
        assertEquals(40, result[1].achievementRate)
    }

    @Test
    fun `다건 progress 조회 시 빈 id 목록이면 빈 결과를 반환한다`() {
        val result = service.getProgresses(emptyList())

        assertTrue(result.isEmpty())
    }

}
