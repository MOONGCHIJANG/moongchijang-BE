package com.moongchijang.domain.admin.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyCloseReason
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyCloseRequestReviewStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.admin.application.dto.AdminOwnerGroupBuyCloseRequestRejectRequest
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AdminOwnerGroupBuyCloseRequestServiceTest {

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var storeStaffRepository: StoreStaffRepository

    @Mock
    private lateinit var notificationEventPublisher: NotificationEventPublisher

    private val clock: Clock = Clock.fixed(Instant.parse("2026-06-03T03:00:00Z"), ZoneId.of("Asia/Seoul"))

    private lateinit var service: AdminOwnerGroupBuyCloseRequestService

    @BeforeEach
    fun setUp() {
        service = AdminOwnerGroupBuyCloseRequestService(
            groupBuyRepository = groupBuyRepository,
            storeStaffRepository = storeStaffRepository,
            notificationEventPublisher = notificationEventPublisher,
            clock = clock
        )
    }

    @Test
    fun `기타 사유 마감 요청을 승인하면 공구를 닫고 승인 알림을 발송한다`() {
        val groupBuy = pendingCloseReviewGroupBuy(id = 21L, status = GroupBuyStatus.IN_PROGRESS)
        `when`(groupBuyRepository.findWithLockById(21L)).thenReturn(Optional.of(groupBuy))
        `when`(storeStaffRepository.findUserIdsByStoreId(groupBuy.store.id)).thenReturn(listOf(91L))

        val result = service.approve(21L)

        assertEquals(GroupBuyCloseRequestReviewStatus.APPROVED, result.reviewStatus)
        assertEquals(GroupBuyStatus.CLOSED, result.groupBuyStatus)
        assertEquals(GroupBuyStatus.CLOSED, groupBuy.status)
        assertEquals(LocalDateTime.of(2026, 6, 3, 12, 0), groupBuy.closeReviewedAt)
        verify(notificationEventPublisher).publishOwnerCloseRequestApproved(
            21L,
            listOf(91L),
            LocalDateTime.of(2026, 6, 3, 12, 0)
        )
    }

    @Test
    fun `기타 사유 마감 요청을 반려하면 공구 상태는 유지하고 반려 사유를 저장한다`() {
        val groupBuy = pendingCloseReviewGroupBuy(id = 22L, status = GroupBuyStatus.ACHIEVED)
        `when`(groupBuyRepository.findWithLockById(22L)).thenReturn(Optional.of(groupBuy))
        `when`(storeStaffRepository.findUserIdsByStoreId(groupBuy.store.id)).thenReturn(listOf(91L))

        val result = service.reject(22L, AdminOwnerGroupBuyCloseRequestRejectRequest("  사유가 불충분합니다  "))

        assertEquals(GroupBuyCloseRequestReviewStatus.REJECTED, result.reviewStatus)
        assertEquals(GroupBuyStatus.ACHIEVED, result.groupBuyStatus)
        assertEquals(GroupBuyStatus.ACHIEVED, groupBuy.status)
        assertEquals("사유가 불충분합니다", groupBuy.closeRequestRejectionReason)
        assertEquals(LocalDateTime.of(2026, 6, 3, 12, 0), groupBuy.closeReviewedAt)
        assertNull(groupBuy.closedByType)
        verify(notificationEventPublisher).publishOwnerCloseRequestRejected(
            22L,
            listOf(91L),
            LocalDateTime.of(2026, 6, 3, 12, 0)
        )
    }

    @Test
    fun `검토 대기 상태가 아니면 승인할 수 없다`() {
        val groupBuy = pendingCloseReviewGroupBuy(id = 23L, status = GroupBuyStatus.IN_PROGRESS).apply {
            closeRequestReviewStatus = GroupBuyCloseRequestReviewStatus.REJECTED
        }
        `when`(groupBuyRepository.findWithLockById(23L)).thenReturn(Optional.of(groupBuy))

        val ex = assertThrows<CustomException> {
            service.approve(23L)
        }

        assertEquals(ErrorCode.GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION, ex.errorCode)
        verify(notificationEventPublisher, never()).publishOwnerCloseRequestApproved(23L, listOf(91L), LocalDateTime.of(2026, 6, 3, 12, 0))
    }

    private fun pendingCloseReviewGroupBuy(
        id: Long,
        status: GroupBuyStatus
    ) = GroupBuyFixture.createGroupBuy(
        id = id,
        status = status,
        deadline = LocalDateTime.of(2026, 6, 5, 23, 59),
    ).apply {
        store.id = 301L
        closeReason = GroupBuyCloseReason.OTHER
        closeReasonDetail = "운영 사정으로 검토 요청"
        closeRequestedAt = LocalDateTime.of(2026, 6, 3, 9, 0)
        closeRequestReviewStatus = GroupBuyCloseRequestReviewStatus.PENDING
    }
}
