package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.notification.infrastructure.aligo.AligoAlimtalkClient
import com.moongchijang.domain.notification.infrastructure.aligo.AligoMessageFormatter
import com.moongchijang.domain.notification.infrastructure.aligo.AligoProperties
import com.moongchijang.support.UserFixture
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class GroupBuyRequestStatusCommandServiceTest {

    @Mock
    private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository

    @Mock
    private lateinit var groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository

    @Mock
    private lateinit var notificationEventPublisher: NotificationEventPublisher

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var aligoAlimtalkClient: AligoAlimtalkClient

    @Mock
    private lateinit var aligoProperties: AligoProperties

    private val service by lazy {
        GroupBuyRequestStatusCommandService(
            groupBuyRequestRepository = groupBuyRequestRepository,
            groupBuyRequestStatusHistoryRepository = groupBuyRequestStatusHistoryRepository,
            notificationEventPublisher = notificationEventPublisher,
            userRepository = userRepository,
            aligoAlimtalkClient = aligoAlimtalkClient,
            aligoProperties = aligoProperties,
        )
    }

    @Test
    fun `요청공구를 거절할 때 거절 상태 반영과 즉시 알림 이벤트 발행`() {
        val now = LocalDateTime.of(2026, 5, 23, 15, 0)
        val request = GroupBuyFixture.createGroupBuyRequest(userId = 7L).apply { id = 301L }

        `when`(groupBuyRequestRepository.findById(301L)).thenReturn(Optional.of(request))
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenAnswer { it.arguments[0] }

        service.rejectRequest(requestId = 301L, reason = "품절", changedAt = now)

        assertEquals(GroupBuyRequestStatus.REJECTED, request.status)
        assertEquals("품절", request.rejectionReason)
        verify(notificationEventPublisher).publishRequestRejected(
            requestId = 301L,
            requesterUserId = 7L,
            occurredAt = now
        )
    }

    @Test
    fun `요청공구 거절 시 공구 개설 실패 알림톡을 발송한다`() {
        val now = LocalDateTime.of(2026, 5, 23, 15, 0)
        val request = GroupBuyFixture.createGroupBuyRequest(
            userId = 7L,
            productName = "황치즈 케이크",
            storeName = "테스트 스토어",
            storeAddress = "서울 강남구 테헤란로 1",
        ).apply {
            id = 401L
        }
        val user = UserFixture.createKakaoUser(id = 7L, nickname = "문치").apply {
            phoneNumber = "01012345678"
        }

        `when`(groupBuyRequestRepository.findById(401L)).thenReturn(Optional.of(request))
        `when`(groupBuyRequestStatusHistoryRepository.save(any())).thenAnswer { it.arguments[0] }
        `when`(userRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(user)
        `when`(aligoProperties.templateCodeGroupBuyOpenFailed).thenReturn("UH_8527")

        service.rejectRequest(requestId = 401L, reason = "심사 탈락", changedAt = now)

        val expectedMessage = AligoMessageFormatter.groupBuyOpenFailed(
            nickname = "문치",
            productName = "황치즈 케이크",
            pickupPlace = "서울 강남구 테헤란로 1",
            pickupDate = request.desiredPickupDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd")),
        )
        verify(aligoAlimtalkClient).send("01012345678", expectedMessage, "UH_8527")
    }
}
