package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyViewerService
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyViewerCountRepository
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
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.eq
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.concurrent.atomic.AtomicReference

@ExtendWith(MockitoExtension::class)
class GroupBuyViewerServiceTest {

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var groupBuyViewerCountRepository: GroupBuyViewerCountRepository

    @InjectMocks
    private lateinit var service: GroupBuyViewerService

    @Test
    fun `heartbeat 비로그인 사용자 session 키 집계 검증`() {
        `when`(groupBuyRepository.existsById(101L)).thenReturn(true)
        val capturedViewerKey = stubTouchAndCaptureViewerKey(returnCount = 1L)

        val result = service.heartbeat(
            groupBuyId = 101L,
            userId = null,
            viewerSessionId = "session-abc-1234"
        )

        assertEquals("session:session-abc-1234", capturedViewerKey.get())
        assertEquals(1, result.activeViewerCount)
        assertFalse(result.showFomoBadge)
        assertEquals(10, result.threshold)
    }

    @Test
    fun `heartbeat 로그인 사용자 user 키 집계 검증`() {
        `when`(groupBuyRepository.existsById(102L)).thenReturn(true)
        val capturedViewerKey = stubTouchAndCaptureViewerKey(returnCount = 3L)

        val result = service.heartbeat(
            groupBuyId = 102L,
            userId = 77L,
            viewerSessionId = "session-ignored"
        )

        assertEquals("user:77", capturedViewerKey.get())
        assertEquals(3, result.activeViewerCount)
        assertFalse(result.showFomoBadge)
    }

    @Test
    fun `활성 조회자 수 10명 이상 FOMO 뱃지 노출 검증`() {
        stubExistsAndCount(groupBuyId = 103L, count = 12L)

        val result = service.heartbeat(
            groupBuyId = 103L,
            userId = null,
            viewerSessionId = "session-fomo-1234"
        )

        assertEquals(12, result.activeViewerCount)
        assertTrue(result.showFomoBadge)
        assertEquals(10, result.threshold)
    }

    @Test
    fun `존재하지 않는 공구 heartbeat 호출 GROUPBUY_NOT_FOUND 예외 발생 검증`() {
        `when`(groupBuyRepository.existsById(999L)).thenReturn(false)

        val ex = assertThrows<CustomException> {
            service.heartbeat(
                groupBuyId = 999L,
                userId = null,
                viewerSessionId = "session-not-found"
            )
        }

        assertEquals(ErrorCode.GROUPBUY_NOT_FOUND, ex.errorCode)
    }

    private fun stubExistsAndCount(groupBuyId: Long, count: Long) {
        `when`(groupBuyRepository.existsById(groupBuyId)).thenReturn(true)
        `when`(
            groupBuyViewerCountRepository.touchAndCount(
                eq(groupBuyId),
                anyString(),
                anyLong(),
                eq(90L)
            )
        ).thenReturn(count)
    }

    private fun stubTouchAndCaptureViewerKey(returnCount: Long): AtomicReference<String?> {
        val capturedViewerKey = AtomicReference<String?>()
        doAnswer {
            capturedViewerKey.set(it.getArgument(1))
            returnCount
        }.`when`(groupBuyViewerCountRepository).touchAndCount(anyLong(), anyString(), anyLong(), anyLong())
        return capturedViewerKey
    }
}
