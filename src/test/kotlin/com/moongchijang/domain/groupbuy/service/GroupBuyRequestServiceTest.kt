package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyOpenRequestService
import com.moongchijang.domain.groupbuy.application.GroupBuyRequestService
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.notification.application.discord.AdminDiscordAlertService
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.support.GroupBuyRequestFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.time.Clock

@ExtendWith(MockitoExtension::class)
class GroupBuyRequestServiceTest {

    @Mock private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository
    @Mock private lateinit var groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository
    @Mock private lateinit var groupBuyRepository: GroupBuyRepository
    @Mock private lateinit var groupBuyOpenRequestService: GroupBuyOpenRequestService
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var clock: Clock
    @Mock private lateinit var adminDiscordAlertService: AdminDiscordAlertService

    @InjectMocks private lateinit var service: GroupBuyRequestService

    @Test
    fun `입점 요청 생성 시 수량과 희망 픽업일 없이 저장한다`() {
        val request = GroupBuyRequestFixture.createRequest()
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(UserFixture.createKakaoUser(id = 1L))
        `when`(clock.instant()).thenReturn(Instant.parse("2026-05-27T04:00:00Z"))
        val saved = GroupBuyRequest(
            user = UserFixture.createKakaoUser(id = 1L),
            storeName = request.storeName,
            productName = request.productName,
        ).apply { id = 42L }
        `when`(groupBuyRequestRepository.save(any())).thenReturn(saved)

        val result = service.create(1L, request)

        assertEquals(42L, result.requestId)
        val captor = ArgumentCaptor.forClass(GroupBuyRequest::class.java)
        verify(groupBuyRequestRepository).save(captor.capture())
        assertEquals(request.storeName, captor.value.storeName)
        assertEquals(request.productName, captor.value.productName)
        verify(groupBuyRequestStatusHistoryRepository).save(any())
    }

    @Test
    fun `요청 상세 응답은 추가 요청사항과 상태 이력을 제공한다`() {
        val request = GroupBuyRequest(
            user = UserFixture.createKakaoUser(id = 1L),
            storeName = "성심당",
            productName = "튀김소보로",
            additionalNote = "주말에도 가능한지 확인 부탁드립니다.",
        ).apply { id = 1L }
        `when`(groupBuyRequestRepository.findById(1L)).thenReturn(java.util.Optional.of(request))
        `when`(groupBuyRequestStatusHistoryRepository.findByGroupBuyRequest_IdOrderByChangedAtAsc(1L))
            .thenReturn(listOf(GroupBuyRequestStatusHistory(request, GroupBuyRequestStatus.IN_REVIEW)))

        val result = service.getDetail(1L, 1L)

        assertEquals("주말에도 가능한지 확인 부탁드립니다.", result.additionalNote)
        assertEquals(1, result.statusHistory.size)
        assertNull(result.rejectionReason)
    }
}
