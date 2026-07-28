package com.moongchijang.domain.refund.application

import com.moongchijang.domain.refund.domain.entity.RefundRequest
import com.moongchijang.domain.refund.domain.entity.RefundRequestStatus
import com.moongchijang.domain.refund.domain.repository.RefundRequestRepository
import com.moongchijang.support.ParticipationFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RefundRequestSyncServiceTest {

    private val refundRequestRepository: RefundRequestRepository = mock(RefundRequestRepository::class.java)

    private val service = RefundRequestSyncService(refundRequestRepository)

    @Test
    fun `환불 승인 동기화 시 요청이 없으면 생성 후 승인 상태로 저장한다`() {
        val participation = createParticipation().apply {
            cancelledAt = LocalDateTime.of(2026, 5, 24, 9, 0)
        }
        val approvedAt = LocalDateTime.of(2026, 5, 24, 10, 30)
        val captor = ArgumentCaptor.forClass(RefundRequest::class.java)

        `when`(refundRequestRepository.findByParticipationId(101L)).thenReturn(null)
        `when`(refundRequestRepository.save(captor.capture())).thenAnswer { it.arguments[0] }

        service.markApproved(participation = participation, approvedAmount = 8_000, at = approvedAt)

        val saved = captor.value
        assertEquals(participation, saved.participation)
        assertEquals(RefundRequestStatus.APPROVED, saved.status)
        assertEquals(participation.totalAmount, saved.requestedAmount)
        assertEquals(participation.cancelledAt, saved.requestedAt)
        assertEquals(8_000, saved.approvedRefundAmount)
        assertEquals(approvedAt, saved.approvedAt)
        assertNull(saved.rejectedReason)
        assertNull(saved.rejectedAt)
    }

    @Test
    fun `환불 거절 동기화 시 기존 요청에 거절 사유와 시각을 반영한다`() {
        val participation = createParticipation()
        val refundRequest = RefundRequest(
            participation = participation,
            requestedAmount = participation.totalAmount,
            requestedAt = LocalDateTime.of(2026, 5, 23, 12, 0),
        )
        val rejectedAt = LocalDateTime.of(2026, 5, 24, 15, 0)

        `when`(refundRequestRepository.findByParticipationId(101L)).thenReturn(refundRequest)

        service.markRejected(participation = participation, reason = "픽업 미진행", at = rejectedAt)

        assertEquals(RefundRequestStatus.REJECTED, refundRequest.status)
        assertEquals("픽업 미진행", refundRequest.rejectedReason)
        assertEquals(rejectedAt, refundRequest.rejectedAt)
        assertEquals(participation.totalAmount, refundRequest.requestedAmount)
    }

    @Test
    fun `환불 완료 동기화 시 createdAt 기준으로 요청을 생성하고 완료 상태로 저장한다`() {
        val participation = createParticipation()
        val completedAt = LocalDateTime.of(2026, 5, 25, 11, 0)
        val captor = ArgumentCaptor.forClass(RefundRequest::class.java)

        `when`(refundRequestRepository.findByParticipationId(101L)).thenReturn(null)
        `when`(refundRequestRepository.save(captor.capture())).thenAnswer { it.arguments[0] }

        service.markCompleted(participation = participation, at = completedAt)

        val saved = captor.value
        assertEquals(RefundRequestStatus.COMPLETED, saved.status)
        assertEquals(LocalDateTime.of(2026, 5, 22, 9, 0), saved.requestedAt)
        assertEquals(completedAt, saved.refundedAt)
    }

    private fun createParticipation() = ParticipationFixture.createParticipation(
        participationId = 101L,
        groupBuyId = 201L,
        quantity = 1,
        totalAmount = 12_000,
        currentQuantity = 10,
        targetQuantity = 20,
        deadline = LocalDateTime.of(2026, 5, 30, 12, 0),
        pickupDate = LocalDate.of(2026, 5, 31),
        pickupTimeStart = LocalTime.of(10, 0),
        createdAt = LocalDateTime.of(2026, 5, 22, 9, 0),
    )
}
