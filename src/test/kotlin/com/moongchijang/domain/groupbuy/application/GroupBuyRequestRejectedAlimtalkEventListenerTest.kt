package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.notification.application.event.NotificationImmediateTriggerEvent
import com.moongchijang.domain.notification.domain.entity.NotificationTriggerType
import com.moongchijang.domain.notification.infrastructure.aligo.AligoAlimtalkClient
import com.moongchijang.domain.notification.infrastructure.aligo.AligoMessageFormatter
import com.moongchijang.domain.notification.infrastructure.aligo.AligoProperties
import com.moongchijang.security.crypto.AesGcmPersonalInfoEncryptor
import com.moongchijang.security.crypto.HmacSha256PersonalInfoHasher
import com.moongchijang.security.crypto.PersonalInfoEncryptionProperties
import com.moongchijang.security.crypto.PersonalInfoManager
import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class GroupBuyRequestRejectedAlimtalkEventListenerTest {

    @Mock
    private lateinit var groupBuyRequestRepository: GroupBuyRequestRepository

    @Mock
    private lateinit var aligoAlimtalkClient: AligoAlimtalkClient

    @Mock
    private lateinit var aligoProperties: AligoProperties

    private val personalInfoProperties = PersonalInfoEncryptionProperties(
        secretKey = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    )
    private val personalInfoManager = PersonalInfoManager(
        AesGcmPersonalInfoEncryptor(personalInfoProperties),
        HmacSha256PersonalInfoHasher(personalInfoProperties),
    )

    private val listener by lazy {
        GroupBuyRequestRejectedAlimtalkEventListener(
            groupBuyRequestRepository = groupBuyRequestRepository,
            aligoAlimtalkClient = aligoAlimtalkClient,
            aligoProperties = aligoProperties,
            personalInfoManager = personalInfoManager,
        )
    }

    @Test
    fun `요청공구 거절 이벤트 수신 시 공구 개설 실패 알림톡을 발송한다`() {
        val request = GroupBuyFixture.createGroupBuyRequest(
            userId = 7L,
            productName = "황치즈 케이크",
            storeAddress = "서울 강남구 테헤란로 1",
        ).apply {
            id = 401L
            user.nickname = "문치"
            user.phoneNumber = "01012345678"
        }

        `when`(groupBuyRequestRepository.findById(401L)).thenReturn(Optional.of(request))
        `when`(aligoProperties.templateCodeGroupBuyOpenFailed).thenReturn("UH_8527")

        listener.on(
            NotificationImmediateTriggerEvent(
                triggerType = NotificationTriggerType.REQUEST_REJECTED_IMMEDIATE,
                targetId = 401L,
                userIds = listOf(7L),
                scheduleKey = "request-rejected:401",
                occurredAt = LocalDateTime.now(),
            )
        )

        val expectedMessage = AligoMessageFormatter.groupBuyOpenFailed(
            nickname = "문치",
            productName = "황치즈 케이크",
            pickupPlace = "서울 강남구 테헤란로 1",
            pickupDate = request.desiredPickupDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
        )
        verify(aligoAlimtalkClient).send("01012345678", expectedMessage, "UH_8527")
    }
}
