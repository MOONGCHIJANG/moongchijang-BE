package com.moongchijang.domain.owner.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyImage
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyImageRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyRequestRejectRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestImage
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestImageRepository
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminOwnerGroupBuyRequestServiceTest {

    @Mock
    private lateinit var ownerGroupBuyRequestRepository: OwnerGroupBuyRequestRepository

    @Mock
    private lateinit var ownerGroupBuyRequestImageRepository: OwnerGroupBuyRequestImageRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var groupBuyImageRepository: GroupBuyImageRepository

    @Mock
    private lateinit var s3ImageReferenceResolver: S3ImageReferenceResolver

    private val clock: Clock = Clock.fixed(Instant.parse("2026-06-01T03:00:00Z"), ZoneId.of("Asia/Seoul"))

    private lateinit var service: AdminOwnerGroupBuyRequestService

    @BeforeEach
    fun setUp() {
        service = AdminOwnerGroupBuyRequestService(
            ownerGroupBuyRequestRepository = ownerGroupBuyRequestRepository,
            ownerGroupBuyRequestImageRepository = ownerGroupBuyRequestImageRepository,
            groupBuyRepository = groupBuyRepository,
            groupBuyImageRepository = groupBuyImageRepository,
            s3ImageReferenceResolver = s3ImageReferenceResolver,
            clock = clock,
        )
    }

    @Test
    fun `사장님 요청을 승인하면 요청 정보로 공구와 이미지를 생성하고 APPROVED로 전환한다`() {
        val requestId = 10L
        val request = ownerRequest(status = OwnerGroupBuyRequestStatus.PENDING).apply { id = requestId }
        val images = listOf(
            OwnerGroupBuyRequestImage(request, "owner/1.jpg", 0),
            OwnerGroupBuyRequestImage(request, "owner/2.jpg", 1),
        )
        `when`(ownerGroupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(request))
        `when`(ownerGroupBuyRequestImageRepository.findAllByRequestIdOrderBySortOrderAsc(requestId)).thenReturn(images)
        `when`(groupBuyRepository.save(any(GroupBuy::class.java))).thenAnswer {
            (it.arguments[0] as GroupBuy).apply { id = 30L }
        }

        val result = service.approve(requestId)

        assertEquals(requestId, result.requestId)
        assertEquals(OwnerGroupBuyRequestStatus.APPROVED, result.status)
        assertEquals(30L, result.groupBuyId)
        assertEquals(OwnerGroupBuyRequestStatus.APPROVED, request.status)
        assertEquals(LocalDateTime.of(2026, 6, 1, 12, 0), request.reviewedAt)
        assertEquals(30L, request.approvedGroupBuy?.id)
        assertNull(request.rejectionReason)

        val groupBuyCaptor = argumentCaptor<GroupBuy>()
        verify(groupBuyRepository).save(groupBuyCaptor.capture())
        assertNull(groupBuyCaptor.value.groupBuyRequest)
        assertEquals(request.store, groupBuyCaptor.value.store)
        assertEquals("두쫀쿠 세트", groupBuyCaptor.value.productName)
        assertEquals("owner-thumb.jpg", groupBuyCaptor.value.thumbnailKey)
        assertEquals(LocalDateTime.of(2026, 6, 1, 12, 0), groupBuyCaptor.value.recruitmentStartAt)
        assertEquals(request.deadline, groupBuyCaptor.value.deadline)
        assertEquals(request.pickupDate, groupBuyCaptor.value.pickupDate)

        val imageCaptor = argumentCaptor<Iterable<GroupBuyImage>>()
        verify(groupBuyImageRepository).saveAll(imageCaptor.capture())
        assertEquals(listOf("owner/1.jpg", "owner/2.jpg"), imageCaptor.value.toList().map { it.imageKey })
    }

    @Test
    fun `이미 처리된 사장님 요청은 승인할 수 없다`() {
        val requestId = 11L
        val request = ownerRequest(status = OwnerGroupBuyRequestStatus.APPROVED).apply { id = requestId }
        `when`(ownerGroupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(request))

        val ex = assertThrows<CustomException> {
            service.approve(requestId)
        }

        assertEquals(ErrorCode.GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION, ex.errorCode)
        verify(groupBuyRepository, never()).save(any(GroupBuy::class.java))
    }

    @Test
    fun `사장님 요청을 반려하면 사유와 검토 시간을 저장한다`() {
        val requestId = 12L
        val request = ownerRequest(status = OwnerGroupBuyRequestStatus.PENDING).apply { id = requestId }
        `when`(ownerGroupBuyRequestRepository.findWithLockById(requestId)).thenReturn(Optional.of(request))

        val result = service.reject(requestId, AdminOwnerGroupBuyRequestRejectRequest("  이미지 품질 보완 필요  "))

        assertEquals(OwnerGroupBuyRequestStatus.REJECTED, result.status)
        assertEquals(OwnerGroupBuyRequestStatus.REJECTED, request.status)
        assertEquals("이미지 품질 보완 필요", request.rejectionReason)
        assertEquals(LocalDateTime.of(2026, 6, 1, 12, 0), request.reviewedAt)
        assertNull(request.approvedGroupBuy)
    }

    private fun ownerRequest(status: OwnerGroupBuyRequestStatus): OwnerGroupBuyRequest =
        OwnerGroupBuyRequest(
            owner = UserFixture.createKakaoUser(id = 1L, nickname = "은서사장").apply {
                role = UserRole.SELLER
                phoneNumber = "01011112222"
            },
            store = store(),
            productName = "두쫀쿠 세트",
            productDescription = "두바이 초코 쿠키 세트",
            originalPrice = 12000,
            price = 9900,
            targetQuantity = 20,
            maxQuantity = 50,
            perUserLimit = 2,
            thumbnailKey = "owner-thumb.jpg",
            deadline = LocalDateTime.of(2026, 6, 10, 18, 0),
            pickupDate = LocalDate.of(2026, 6, 12),
            pickupTimeStart = LocalTime.of(12, 0),
            pickupTimeEnd = LocalTime.of(18, 0),
            pickupLocation = "서울 성동구 성수이로 1",
            pickupContact = "01012345678",
            status = status,
        )

    private fun store(): Store =
        Store(
            name = "뭉치장 베이커리",
            address = "서울 성동구 성수이로 1",
            phoneNumber = "01012345678",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN,
        ).apply { id = 20L }

    private inline fun <reified T> argumentCaptor() = org.mockito.ArgumentCaptor.forClass(T::class.java)
}
