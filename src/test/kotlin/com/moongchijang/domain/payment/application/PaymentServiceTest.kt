package com.moongchijang.domain.payment.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.infrastructure.lock.RedisLockUtil
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.notification.application.discord.AdminDiscordAlertService
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.payment.application.dto.CancelParticipationRequest
import com.moongchijang.domain.payment.application.dto.CompletePortOnePaymentRequest
import com.moongchijang.domain.payment.application.dto.CreatePaymentOrderRequest
import com.moongchijang.domain.payment.application.dto.PortOneWebhookRequest
import com.moongchijang.domain.payment.application.port.PortOnePaymentPort
import com.moongchijang.domain.payment.application.port.PortOnePaymentResult
import com.moongchijang.domain.payment.domain.entity.Payment
import com.moongchijang.domain.payment.domain.entity.PaymentOrder
import com.moongchijang.domain.payment.domain.entity.PaymentOrderStatus
import com.moongchijang.domain.payment.domain.entity.PaymentStatus
import com.moongchijang.domain.payment.domain.repository.PaymentOrderRepository
import com.moongchijang.domain.payment.domain.repository.PaymentRepository
import com.moongchijang.domain.refund.application.RefundRequestSyncService
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.config.PortOneProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class PaymentServiceTest {

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @Mock
    private lateinit var favoriteRepository: FavoriteRepository

    @Mock
    private lateinit var storeStaffRepository: StoreStaffRepository

    @Mock
    private lateinit var paymentOrderRepository: PaymentOrderRepository

    @Mock
    private lateinit var paymentRepository: PaymentRepository

    @Mock
    private lateinit var paymentAuditLogService: PaymentAuditLogService

    @Mock
    private lateinit var portOnePaymentPort: PortOnePaymentPort

    @Mock
    private lateinit var transactionManager: PlatformTransactionManager

    @Mock
    private lateinit var transactionStatus: TransactionStatus

    @Mock
    private lateinit var redisLockUtil: RedisLockUtil

    @Mock
    private lateinit var notificationEventPublisher: NotificationEventPublisher

    @Mock
    private lateinit var adminDiscordAlertService: AdminDiscordAlertService

    @Mock
    private lateinit var refundRequestSyncService: RefundRequestSyncService

    @Mock
    private lateinit var s3ImageReferenceResolver: S3ImageReferenceResolver

    private val clock: Clock = Clock.fixed(Instant.parse("2026-05-23T03:00:00Z"), ZoneOffset.UTC)

    private val portOneProperties = PortOneProperties(
        storeId = "store-test",
        channelKey = "channel-test",
        apiSecret = "secret-test",
    )

    private val service: PaymentService by lazy {
        PaymentService(
            groupBuyRepository = groupBuyRepository,
            userRepository = userRepository,
            participationRepository = participationRepository,
            favoriteRepository = favoriteRepository,
            storeStaffRepository = storeStaffRepository,
            paymentOrderRepository = paymentOrderRepository,
            paymentRepository = paymentRepository,
            paymentAuditLogService = paymentAuditLogService,
            portOnePaymentPort = portOnePaymentPort,
            portOneProperties = portOneProperties,
            transactionManager = transactionManager,
            redisLockUtil = redisLockUtil,
            notificationEventPublisher = notificationEventPublisher,
            adminDiscordAlertService = adminDiscordAlertService,
            refundRequestSyncService = refundRequestSyncService,
            s3ImageReferenceResolver = s3ImageReferenceResolver,
            clock = clock
        )
    }

    @Test
    fun `체크아웃 정보는 수수료 0원으로 금액 계산`() {
        val groupBuy = createGroupBuy(price = 6000)
        `when`(groupBuyRepository.findWithStoreById(10L)).thenReturn(Optional.of(groupBuy))

        val result = service.getCheckoutInfo(10L, 2)

        assertEquals(12000, result.productAmount)
        assertEquals(0, result.feeAmount)
        assertEquals(12000, result.totalAmount)
        assertEquals(64, result.remainingQuantity)
    }

    @Test
    fun `ACHIEVED 상태 공구도 체크아웃 정보 조회 가능`() {
        val groupBuy = createGroupBuy(status = GroupBuyStatus.ACHIEVED, currentQuantity = 50, maxQuantity = 100)
        `when`(groupBuyRepository.findWithStoreById(10L)).thenReturn(Optional.of(groupBuy))

        val result = service.getCheckoutInfo(10L, 2)

        assertEquals(10L, result.groupBuyId)
        assertEquals(50, groupBuy.currentQuantity)
        assertEquals(50, result.remainingQuantity)
    }

    @Test
    fun `1인 구매 제한을 초과하면 체크아웃 정보 조회 실패`() {
        val groupBuy = createGroupBuy().apply { perUserLimit = 2 }
        `when`(groupBuyRepository.findWithStoreById(10L)).thenReturn(Optional.of(groupBuy))

        val ex = assertThrows<CustomException> {
            service.getCheckoutInfo(10L, 3)
        }

        assertEquals(ErrorCode.PAYMENT_PER_USER_LIMIT_EXCEEDED, ex.errorCode)
    }

    @Test
    fun `필수 동의가 없으면 결제 주문 생성 실패`() {
        val request = createOrderRequest(agreedNoWithdrawal = false)

        val ex = assertThrows<CustomException> {
            service.createPaymentOrder(10L, 1L, request)
        }

        assertEquals(ErrorCode.PAYMENT_AGREEMENT_REQUIRED, ex.errorCode)
    }

    @Test
    fun `결제 주문 생성 성공`() {
        val user = UserFixture.createKakaoUser(id = 1L, nickname = "은서")
        val groupBuy = createGroupBuy()
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(user)
        `when`(groupBuyRepository.findWithLockById(10L)).thenReturn(Optional.of(groupBuy))
        `when`(participationRepository.existsByUserIdAndGroupBuyId(1L, 10L)).thenReturn(false)
        `when`(paymentOrderRepository.save(any(PaymentOrder::class.java))).thenAnswer { it.arguments[0] }

        val result = service.createPaymentOrder(10L, 1L, createOrderRequest(quantity = 3))

        assertTrue(result.paymentId.startsWith("MCJ-10-"))
        assertEquals("store-test", result.storeId)
        assertEquals("channel-test", result.channelKey)
        assertEquals("두쫀쿠 3개", result.orderName)
        assertEquals(18000, result.amount)
        assertEquals("은서", result.customerName)
    }

    @Test
    fun `ACHIEVED 상태 공구도 결제 주문 생성 가능`() {
        val user = UserFixture.createKakaoUser(id = 1L, nickname = "은서")
        val groupBuy = createGroupBuy(status = GroupBuyStatus.ACHIEVED, currentQuantity = 50, maxQuantity = 100)
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(user)
        `when`(groupBuyRepository.findWithLockById(10L)).thenReturn(Optional.of(groupBuy))
        `when`(participationRepository.existsByUserIdAndGroupBuyId(1L, 10L)).thenReturn(false)
        `when`(paymentOrderRepository.save(any(PaymentOrder::class.java))).thenAnswer { it.arguments[0] }

        val result = service.createPaymentOrder(10L, 1L, createOrderRequest(quantity = 2))

        assertTrue(result.paymentId.startsWith("MCJ-10-"))
        assertEquals("두쫀쿠 2개", result.orderName)
        assertEquals(12000, result.amount)
    }

    @Test
    fun `1인 구매 제한을 초과하면 결제 주문 생성 실패`() {
        val user = UserFixture.createKakaoUser(id = 1L, nickname = "은서")
        val groupBuy = createGroupBuy().apply { perUserLimit = 2 }
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(user)
        `when`(groupBuyRepository.findWithLockById(10L)).thenReturn(Optional.of(groupBuy))

        val ex = assertThrows<CustomException> {
            service.createPaymentOrder(10L, 1L, createOrderRequest(quantity = 3))
        }

        assertEquals(ErrorCode.PAYMENT_PER_USER_LIMIT_EXCEEDED, ex.errorCode)
    }

    @Test
    fun `deadline 이 지나도 ACHIEVED 상태 공구는 체크아웃 정보 조회 가능`() {
        val groupBuy = createGroupBuy(
            status = GroupBuyStatus.ACHIEVED,
            currentQuantity = 50,
            maxQuantity = 100,
        ).apply {
            deadline = LocalDateTime.now().minusMinutes(1)
        }
        `when`(groupBuyRepository.findWithStoreById(10L)).thenReturn(Optional.of(groupBuy))

        val result = service.getCheckoutInfo(10L, 1)
        assertEquals(10L, result.groupBuyId)
    }

    @Test
    fun `deadline 이 지나도 ACHIEVED 상태 공구는 결제 주문 생성 가능`() {
        val user = UserFixture.createKakaoUser(id = 1L, nickname = "은서")
        val groupBuy = createGroupBuy(status = GroupBuyStatus.ACHIEVED, currentQuantity = 50, maxQuantity = 100).apply {
            deadline = LocalDateTime.now().minusMinutes(1)
        }
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(user)
        `when`(groupBuyRepository.findWithLockById(10L)).thenReturn(Optional.of(groupBuy))
        `when`(participationRepository.existsByUserIdAndGroupBuyId(1L, 10L)).thenReturn(false)
        `when`(paymentOrderRepository.save(any(PaymentOrder::class.java))).thenAnswer { it.arguments[0] }

        val result = service.createPaymentOrder(10L, 1L, createOrderRequest(quantity = 1))
        assertEquals("두쫀쿠 1개", result.orderName)
    }

    @Test
    fun `COMPLETED 상태 공구는 체크아웃 정보 조회 불가`() {
        val groupBuy = createGroupBuy(status = GroupBuyStatus.COMPLETED, currentQuantity = 100, maxQuantity = 100)
        `when`(groupBuyRepository.findWithStoreById(10L)).thenReturn(Optional.of(groupBuy))

        val ex = assertThrows<CustomException> {
            service.getCheckoutInfo(10L, 1)
        }

        assertEquals(ErrorCode.PAYMENT_GROUPBUY_NOT_AVAILABLE, ex.errorCode)
    }

    @Test
    fun `포트원 결제 완료 시 참여 생성 및 현재 수량 증가`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(currentQuantity = 36)
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 2)
        val approvedAt = LocalDateTime.of(2026, 5, 17, 12, 0)
        stubTransaction()
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)
        `when`(paymentOrderRepository.findByOrderIdForUpdate("MCJ-10-test")).thenReturn(order)
        `when`(groupBuyRepository.increaseCurrentQuantityIfAvailable(10L, 2)).thenAnswer {
            groupBuy.currentQuantity += 2
            1
        }
        `when`(groupBuyRepository.findById(10L)).thenReturn(Optional.of(groupBuy))
        `when`(participationRepository.existsByUserIdAndGroupBuyId(1L, 10L)).thenReturn(false)
        `when`(portOnePaymentPort.getPayment("MCJ-10-test"))
            .thenReturn(PortOnePaymentResult("MCJ-10-test", "PAID", 12000, "CARD", approvedAt))
        `when`(participationRepository.save(any(Participation::class.java))).thenAnswer {
            (it.arguments[0] as Participation).apply { id = 99L }
        }
        `when`(paymentOrderRepository.save(any(PaymentOrder::class.java))).thenAnswer { it.arguments[0] }

        val result = service.completePortOnePayment(CompletePortOnePaymentRequest("MCJ-10-test", 12000), 1L)

        assertEquals(99L, result.participationId)
        assertEquals(ParticipationStatus.PAID_WAITING_GOAL, result.participationStatus)
        assertEquals(38, groupBuy.currentQuantity)
        assertEquals(PaymentOrderStatus.APPROVED, order.status)
        verify(redisLockUtil).lockKey(10L)
        verify(redisLockUtil).tryLockOrThrow("groupBuy:10", LOCK_WAIT_MS, LOCK_LEASE_MS)
        verify(redisLockUtil).unlock("groupBuy:10", "lock-token")
        verify(paymentRepository).save(any())
    }

    @Test
    fun `락 획득 실패 시 GROUPBUY_LOCK_ACQUISITION_FAILED 예외를 전파한다`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(currentQuantity = 36)
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 2)
        stubTransaction()
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)
        `when`(portOnePaymentPort.getPayment("MCJ-10-test"))
            .thenReturn(PortOnePaymentResult("MCJ-10-test", "PAID", 12000, "CARD", LocalDateTime.now()))
        `when`(redisLockUtil.lockKey(10L)).thenReturn("groupBuy:10")
        `when`(redisLockUtil.tryLockOrThrow("groupBuy:10", LOCK_WAIT_MS, LOCK_LEASE_MS))
            .thenThrow(CustomException(ErrorCode.GROUPBUY_LOCK_ACQUISITION_FAILED))

        val ex = assertThrows<CustomException> {
            service.completePortOnePayment(CompletePortOnePaymentRequest("MCJ-10-test", 12000), 1L)
        }

        assertEquals(ErrorCode.GROUPBUY_LOCK_ACQUISITION_FAILED, ex.errorCode)
        verify(redisLockUtil).lockKey(10L)
    }

    @Test
    fun `포트원 결제 조회 실패는 주문을 실패 확정하지 않고 재시도와 웹훅 처리를 열어둔다`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(currentQuantity = 36)
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 1)
        stubTransaction()
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)
        `when`(portOnePaymentPort.getPayment("MCJ-10-test"))
            .thenThrow(CustomException(ErrorCode.PAYMENT_APPROVAL_FAILED))

        val ex = assertThrows<CustomException> {
            service.completePortOnePayment(CompletePortOnePaymentRequest("MCJ-10-test", 6000), 1L)
        }

        assertEquals(ErrorCode.PAYMENT_APPROVAL_FAILED, ex.errorCode)
        assertEquals(PaymentOrderStatus.READY, order.status)
        verify(paymentOrderRepository, never()).save(order)
    }

    @Test
    fun `결제 완료 검증은 주문 소유자만 처리할 수 있다`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy()
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 1)
        stubTransaction()
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)

        val ex = assertThrows<CustomException> {
            service.completePortOnePayment(CompletePortOnePaymentRequest("MCJ-10-test", 6000), 2L)
        }

        assertEquals(ErrorCode.PAYMENT_ORDER_FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `결제 완료로 목표 수량에 도달하면 참여 확정 상태로 생성`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(currentQuantity = 49)
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 1)
        val approvedAt = LocalDateTime.of(2026, 5, 17, 12, 0)
        stubTransaction()
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)
        `when`(paymentOrderRepository.findByOrderIdForUpdate("MCJ-10-test")).thenReturn(order)
        `when`(groupBuyRepository.increaseCurrentQuantityIfAvailable(10L, 1)).thenAnswer {
            groupBuy.currentQuantity += 1
            1
        }
        `when`(groupBuyRepository.findById(10L)).thenReturn(Optional.of(groupBuy))
        `when`(participationRepository.existsByUserIdAndGroupBuyId(1L, 10L)).thenReturn(false)
        `when`(portOnePaymentPort.getPayment("MCJ-10-test"))
            .thenReturn(PortOnePaymentResult("MCJ-10-test", "PAID", 6000, "CARD", approvedAt))
        `when`(participationRepository.save(any(Participation::class.java))).thenAnswer {
            (it.arguments[0] as Participation).apply { id = 100L }
        }
        `when`(paymentOrderRepository.save(any(PaymentOrder::class.java))).thenAnswer { it.arguments[0] }

        val result = service.completePortOnePayment(CompletePortOnePaymentRequest("MCJ-10-test", 6000), 1L)

        assertEquals(ParticipationStatus.CONFIRMED, result.participationStatus)
        assertEquals(GroupBuyStatus.ACHIEVED, groupBuy.status)
        verify(notificationEventPublisher).publishRequestTargetAchieved(20L, 1L, approvedAt)
    }

    @Test
    fun `요청공구에 다른 사용자가 참여할 때 requestId 기준 새 참여자 알림 발행`() {
        val requester = UserFixture.createKakaoUser(id = 1L)
        val participantUser = UserFixture.createKakaoUser(id = 2L)
        val groupBuy = createGroupBuy().apply {
            groupBuyRequest = GroupBuyRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = 1L),
                storeName = "뭉치장 베이커리",
                storeAddress = "서울 성동구",
                productName = "두쫀쿠",
                desiredQuantity = 50,
                desiredPickupDate = LocalDate.now().plusDays(5)
            ).apply { id = 320L }
        }
        val order = createPaymentOrder(user = participantUser, groupBuy = groupBuy, quantity = 1)
        val approvedAt = LocalDateTime.of(2026, 5, 17, 12, 0)

        stubTransaction()
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)
        `when`(paymentOrderRepository.findByOrderIdForUpdate("MCJ-10-test")).thenReturn(order)
        `when`(groupBuyRepository.increaseCurrentQuantityIfAvailable(10L, 1)).thenAnswer {
            groupBuy.currentQuantity += 1
            1
        }
        `when`(groupBuyRepository.findById(10L)).thenReturn(Optional.of(groupBuy))
        `when`(participationRepository.existsByUserIdAndGroupBuyId(2L, 10L)).thenReturn(false)
        `when`(portOnePaymentPort.getPayment("MCJ-10-test"))
            .thenReturn(PortOnePaymentResult("MCJ-10-test", "PAID", 6000, "CARD", approvedAt))
        `when`(participationRepository.save(any(Participation::class.java))).thenAnswer {
            (it.arguments[0] as Participation).apply { id = 210L }
        }
        `when`(paymentOrderRepository.save(any(PaymentOrder::class.java))).thenAnswer { it.arguments[0] }

        service.completePortOnePayment(CompletePortOnePaymentRequest("MCJ-10-test", 6000), 2L)

        verify(notificationEventPublisher).publishRequestNewParticipant(320L, 1L, 210L, approvedAt)
    }

    @Test
    fun `결제 완료 시 max 수량 도달하면 COMPLETED 상태 전이`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(
            status = GroupBuyStatus.ACHIEVED,
            currentQuantity = 99,
            maxQuantity = 100
        )
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 1)
        stubTransaction()
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)
        `when`(paymentOrderRepository.findByOrderIdForUpdate("MCJ-10-test")).thenReturn(order)
        `when`(groupBuyRepository.increaseCurrentQuantityIfAvailable(10L, 1)).thenAnswer {
            groupBuy.currentQuantity += 1
            1
        }
        `when`(groupBuyRepository.findById(10L)).thenReturn(Optional.of(groupBuy))
        `when`(participationRepository.existsByUserIdAndGroupBuyId(1L, 10L)).thenReturn(false)
        `when`(portOnePaymentPort.getPayment("MCJ-10-test"))
            .thenReturn(PortOnePaymentResult("MCJ-10-test", "PAID", 6000, "CARD", LocalDateTime.now()))
        `when`(participationRepository.save(any(Participation::class.java))).thenAnswer {
            (it.arguments[0] as Participation).apply { id = 101L }
        }
        `when`(paymentOrderRepository.save(any(PaymentOrder::class.java))).thenAnswer { it.arguments[0] }

        val result = service.completePortOnePayment(CompletePortOnePaymentRequest("MCJ-10-test", 6000), 1L)

        assertEquals(ParticipationStatus.CONFIRMED, result.participationStatus)
        assertEquals(GroupBuyStatus.COMPLETED, groupBuy.status)
    }

    @Test
    fun `승인 시점에 최대 수량을 초과하면 실패`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(currentQuantity = 99, maxQuantity = 100)
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 2)
        stubTransaction()
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)
        `when`(paymentOrderRepository.findByOrderIdForUpdate("MCJ-10-test")).thenReturn(order)
        `when`(portOnePaymentPort.getPayment("MCJ-10-test"))
            .thenReturn(PortOnePaymentResult("MCJ-10-test", "PAID", 12000, "CARD", LocalDateTime.now()))
        `when`(participationRepository.existsByUserIdAndGroupBuyId(1L, 10L)).thenReturn(false)
        `when`(groupBuyRepository.increaseCurrentQuantityIfAvailable(10L, 2)).thenReturn(0)

        val ex = assertThrows<CustomException> {
            service.completePortOnePayment(CompletePortOnePaymentRequest("MCJ-10-test", 12000), 1L)
        }

        assertEquals(ErrorCode.PAYMENT_QUANTITY_EXCEEDED, ex.errorCode)
    }

    @Test
    fun `웹훅 결제 성공도 서버 검증 후 참여 생성`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(currentQuantity = 36)
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 1)
        stubTransaction()
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)
        `when`(paymentOrderRepository.findByOrderIdForUpdate("MCJ-10-test")).thenReturn(order)
        `when`(portOnePaymentPort.getPayment("MCJ-10-test"))
            .thenReturn(PortOnePaymentResult("MCJ-10-test", "PAID", 6000, "CARD", LocalDateTime.now()))
        `when`(groupBuyRepository.increaseCurrentQuantityIfAvailable(10L, 1)).thenAnswer {
            groupBuy.currentQuantity += 1
            1
        }
        `when`(groupBuyRepository.findById(10L)).thenReturn(Optional.of(groupBuy))
        `when`(participationRepository.existsByUserIdAndGroupBuyId(1L, 10L)).thenReturn(false)
        `when`(participationRepository.save(any(Participation::class.java))).thenAnswer {
            (it.arguments[0] as Participation).apply { id = 100L }
        }
        `when`(paymentOrderRepository.save(any(PaymentOrder::class.java))).thenAnswer { it.arguments[0] }

        service.handlePortOneWebhook(
            PortOneWebhookRequest(type = "Transaction.Paid", storeId = "store-test", paymentId = "MCJ-10-test")
        )

        assertEquals(PaymentOrderStatus.APPROVED, order.status)
        assertEquals(37, groupBuy.currentQuantity)
        verify(paymentRepository).save(any())
    }

    @Test
    fun `웹훅 실패 상태는 준비 중 주문을 실패 처리`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy()
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 1)
        stubTransaction()
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)
        `when`(paymentOrderRepository.findByOrderIdForUpdate("MCJ-10-test")).thenReturn(order)
        `when`(portOnePaymentPort.getPayment("MCJ-10-test"))
            .thenReturn(PortOnePaymentResult("MCJ-10-test", "FAILED", 6000, "CARD", null))

        service.handlePortOneWebhook(PortOneWebhookRequest(storeId = "store-test", paymentId = "MCJ-10-test"))

        assertEquals(PaymentOrderStatus.FAILED, order.status)
    }

    @Test
    fun `웹훅 취소 상태는 결제와 참여를 환불 처리`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy()
        val (order, payment) = createApprovedOrderAndPayment(user, groupBuy)
        val participation = Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 6000,
            feeAmount = 0,
            totalAmount = 6000,
            status = ParticipationStatus.PAID_WAITING_GOAL,
        )
        val cancelledAt = LocalDateTime.of(2026, 5, 18, 10, 0)
        stubTransaction()
        stubCancelledWebhook(order, cancelledAt)
        `when`(paymentRepository.findByPaymentOrderOrderId("MCJ-10-test")).thenReturn(payment)
        `when`(participationRepository.findByUserIdAndGroupBuyId(1L, 10L)).thenReturn(participation)
        `when`(groupBuyRepository.findWithLockById(10L)).thenReturn(Optional.of(groupBuy))

        service.handlePortOneWebhook(PortOneWebhookRequest(storeId = "store-test", paymentId = "MCJ-10-test"))

        assertEquals(PaymentOrderStatus.CANCELLED, order.status)
        assertEquals(PaymentStatus.CANCELLED, payment.status)
        assertEquals(ParticipationStatus.REFUNDED, participation.status)
        assertEquals(cancelledAt, participation.refundedAt)
        assertEquals(35, groupBuy.currentQuantity)
    }

    @Test
    fun `웹훅 취소 재요청 시 이미 환불된 참여는 멱등 처리`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(currentQuantity = 35)
        val (order, payment) = createApprovedOrderAndPayment(user, groupBuy)
        val refundedAt = LocalDateTime.of(2026, 5, 18, 10, 0)
        val participation = Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 6000,
            feeAmount = 0,
            totalAmount = 6000,
            status = ParticipationStatus.REFUNDED,
            refundedAt = refundedAt,
        )
        stubTransaction()
        stubCancelledWebhook(order, LocalDateTime.of(2026, 5, 19, 10, 0))
        `when`(paymentRepository.findByPaymentOrderOrderId("MCJ-10-test")).thenReturn(payment)
        `when`(participationRepository.findByUserIdAndGroupBuyId(1L, 10L)).thenReturn(participation)

        service.handlePortOneWebhook(PortOneWebhookRequest(storeId = "store-test", paymentId = "MCJ-10-test"))

        assertEquals(35, groupBuy.currentQuantity)
        assertEquals(ParticipationStatus.REFUNDED, participation.status)
        assertEquals(refundedAt, participation.refundedAt)
    }

    @Test
    fun `웹훅 취소 시 달성 완료 공구는 환불 거절`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(currentQuantity = 50, status = GroupBuyStatus.ACHIEVED)
        val (order, payment) = createApprovedOrderAndPayment(user, groupBuy)
        val participation = Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 6000,
            feeAmount = 0,
            totalAmount = 6000,
            status = ParticipationStatus.CONFIRMED,
        )
        stubTransaction()
        stubCancelledWebhook(order, LocalDateTime.of(2026, 5, 19, 10, 0))
        `when`(paymentRepository.findByPaymentOrderOrderId("MCJ-10-test")).thenReturn(payment)
        `when`(participationRepository.findByUserIdAndGroupBuyId(1L, 10L)).thenReturn(participation)
        `when`(groupBuyRepository.findWithLockById(10L)).thenReturn(Optional.of(groupBuy))

        val ex = assertThrows<CustomException> {
            service.handlePortOneWebhook(PortOneWebhookRequest(storeId = "store-test", paymentId = "MCJ-10-test"))
        }

        assertEquals(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED_AFTER_ACHIEVED, ex.errorCode)
        assertEquals(PaymentOrderStatus.APPROVED, order.status)
        assertEquals(PaymentStatus.APPROVED, payment.status)
        assertEquals(ParticipationStatus.CONFIRMED, participation.status)
        assertEquals(50, groupBuy.currentQuantity)
    }

    @Test
    fun `달성 전 참여는 취소 사유 저장 후 환불 처리`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy()
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 2).apply {
            approve(LocalDateTime.now().minusMinutes(5))
        }
        val payment = Payment(
            paymentOrder = order,
            pgPaymentId = "portone-payment-id",
            orderId = order.orderId,
            amount = order.totalAmount,
            method = "CARD",
            approvedAt = order.approvedAt!!,
        )
        val participation = Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 2,
            productAmount = 12000,
            feeAmount = 0,
            totalAmount = 12000,
            status = ParticipationStatus.PAID_WAITING_GOAL,
        ).apply { id = 77L }
        val cancelledAt = LocalDateTime.of(2026, 5, 18, 11, 0)
        stubTransaction()
        `when`(participationRepository.findById(77L)).thenReturn(Optional.of(participation))
        `when`(participationRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(participation))
        `when`(paymentOrderRepository.findByUserIdAndGroupBuyId(1L, 10L)).thenReturn(order)
        `when`(paymentOrderRepository.findByOrderIdForUpdate("MCJ-10-test")).thenReturn(order)
        `when`(paymentRepository.findByPaymentOrderOrderId("MCJ-10-test")).thenReturn(payment)
        `when`(groupBuyRepository.findWithLockById(10L)).thenReturn(Optional.of(groupBuy))
        `when`(portOnePaymentPort.cancelPayment("portone-payment-id", "OTHER: 시간이 맞지 않습니다"))
            .thenReturn(
                PortOnePaymentResult(
                    paymentId = "portone-payment-id",
                    status = "CANCELLED",
                    totalAmount = 12000,
                    method = "CARD",
                    paidAt = order.approvedAt,
                    cancelledAt = cancelledAt,
                )
            )

        val result = service.cancelParticipation(
            participationId = 77L,
            userId = 1L,
            request = CancelParticipationRequest(
                reason = ParticipationCancelReason.OTHER,
                reasonDetail = " 시간이 맞지 않습니다 ",
            )
        )

        assertEquals(77L, result.participationId)
        assertEquals(ParticipationStatus.REFUNDED, result.status)
        assertEquals(PaymentOrderStatus.CANCELLED, order.status)
        assertEquals(PaymentStatus.CANCELLED, payment.status)
        assertEquals(ParticipationStatus.REFUNDED, participation.status)
        assertEquals(ParticipationCancelReason.OTHER, participation.cancelReason)
        assertEquals("시간이 맞지 않습니다", participation.cancelReasonDetail)
        assertEquals(cancelledAt, participation.cancelledAt)
        assertEquals(cancelledAt, participation.refundedAt)
        assertEquals(cancelledAt, result.cancelledAt)
        assertEquals(34, groupBuy.currentQuantity)
    }

    @Test
    fun `환불대기 참여는 PG 취소 성공 시 환불완료 처리`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(status = GroupBuyStatus.FAILED)
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 2).apply {
            approve(LocalDateTime.now().minusMinutes(5))
        }
        val payment = Payment(
            paymentOrder = order,
            pgPaymentId = "portone-payment-id",
            orderId = order.orderId,
            amount = order.totalAmount,
            method = "CARD",
            approvedAt = order.approvedAt!!,
        )
        val participation = Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 2,
            productAmount = 12_000,
            feeAmount = 0,
            totalAmount = 12_000,
            status = ParticipationStatus.REFUND_PENDING,
            cancelledAt = LocalDateTime.of(2026, 5, 18, 10, 0),
        ).apply { id = 88L }
        val refundedAt = LocalDateTime.of(2026, 5, 18, 11, 0)
        val pageable = pendingRefundPageable()
        stubTransaction()
        `when`(
            participationRepository.findByStatusOrderByCancelledAtAscCreatedAtAsc(
                ParticipationStatus.REFUND_PENDING,
                pageable
            )
        ).thenReturn(listOf(participation))
        `when`(participationRepository.findByIdForUpdate(88L)).thenReturn(Optional.of(participation))
        `when`(paymentOrderRepository.findByUserIdAndGroupBuyIdForUpdate(1L, 10L)).thenReturn(order)
        `when`(paymentOrderRepository.findByOrderIdForUpdate(order.orderId)).thenReturn(order)
        `when`(paymentRepository.findByPaymentOrderOrderId(order.orderId)).thenReturn(payment)
        `when`(portOnePaymentPort.cancelPayment("portone-payment-id", "MINIMUM_QUANTITY_NOT_MET", 12_000))
            .thenReturn(
                PortOnePaymentResult(
                    paymentId = "portone-payment-id",
                    status = "CANCELLED",
                    totalAmount = 12_000,
                    method = "CARD",
                    paidAt = order.approvedAt,
                    cancelledAt = refundedAt,
                )
            )

        val result = service.processPendingRefunds()

        assertEquals(1, result.targetCount)
        assertEquals(1, result.successCount)
        assertEquals(0, result.failedCount)
        assertEquals(PaymentOrderStatus.CANCELLED, order.status)
        assertEquals(PaymentStatus.CANCELLED, payment.status)
        assertEquals(ParticipationStatus.REFUNDED, participation.status)
        assertEquals(refundedAt, participation.refundedAt)
    }

    @Test
    fun `환불대기 참여는 PG 취소 실패 시 대기 상태를 유지`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy(status = GroupBuyStatus.FAILED)
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 2).apply {
            approve(LocalDateTime.now().minusMinutes(5))
        }
        val payment = Payment(
            paymentOrder = order,
            pgPaymentId = "portone-payment-id",
            orderId = order.orderId,
            amount = order.totalAmount,
            method = "CARD",
            approvedAt = order.approvedAt!!,
        )
        val participation = Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 2,
            productAmount = 12_000,
            feeAmount = 0,
            totalAmount = 12_000,
            status = ParticipationStatus.REFUND_PENDING,
            cancelledAt = LocalDateTime.of(2026, 5, 18, 10, 0),
        ).apply { id = 89L }
        val pageable = pendingRefundPageable()
        stubTransaction()
        `when`(
            participationRepository.findByStatusOrderByCancelledAtAscCreatedAtAsc(
                ParticipationStatus.REFUND_PENDING,
                pageable
            )
        ).thenReturn(listOf(participation))
        `when`(participationRepository.findByIdForUpdate(89L)).thenReturn(Optional.of(participation))
        `when`(paymentOrderRepository.findByUserIdAndGroupBuyIdForUpdate(1L, 10L)).thenReturn(order)
        `when`(paymentRepository.findByPaymentOrderOrderId(order.orderId)).thenReturn(payment)
        `when`(portOnePaymentPort.cancelPayment("portone-payment-id", "MINIMUM_QUANTITY_NOT_MET", 12_000))
            .thenReturn(
                PortOnePaymentResult(
                    paymentId = "portone-payment-id",
                    status = "FAILED",
                    totalAmount = 12_000,
                    method = "CARD",
                    paidAt = order.approvedAt,
                )
            )

        val result = service.processPendingRefunds()

        assertEquals(1, result.targetCount)
        assertEquals(0, result.successCount)
        assertEquals(1, result.failedCount)
        assertEquals(PaymentOrderStatus.APPROVED, order.status)
        assertEquals(PaymentStatus.APPROVED, payment.status)
        assertEquals(ParticipationStatus.REFUND_PENDING, participation.status)
        assertEquals(null, participation.refundedAt)
    }

    @Test
    fun `환불대기 처리 중 개별 예외가 발생해도 다음 대기 건을 처리한다`() {
        val firstUser = UserFixture.createKakaoUser(id = 1L)
        val secondUser = UserFixture.createKakaoUser(id = 2L)
        val groupBuy = createGroupBuy(status = GroupBuyStatus.FAILED)
        val firstParticipation = Participation(
            user = firstUser,
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 6_000,
            feeAmount = 0,
            totalAmount = 6_000,
            status = ParticipationStatus.REFUND_PENDING,
            cancelledAt = LocalDateTime.of(2026, 5, 18, 10, 0),
        ).apply { id = 90L }
        val secondOrder = createPaymentOrder(user = secondUser, groupBuy = groupBuy, quantity = 1).apply {
            approve(LocalDateTime.now().minusMinutes(5))
        }
        val secondPayment = Payment(
            paymentOrder = secondOrder,
            pgPaymentId = "second-portone-payment-id",
            orderId = secondOrder.orderId,
            amount = secondOrder.totalAmount,
            method = "CARD",
            approvedAt = secondOrder.approvedAt!!,
        )
        val secondParticipation = Participation(
            user = secondUser,
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 6_000,
            feeAmount = 0,
            totalAmount = 6_000,
            status = ParticipationStatus.REFUND_PENDING,
            cancelledAt = LocalDateTime.of(2026, 5, 18, 10, 1),
        ).apply { id = 91L }
        val refundedAt = LocalDateTime.of(2026, 5, 18, 11, 0)
        val pageable = pendingRefundPageable()
        stubTransaction()
        `when`(
            participationRepository.findByStatusOrderByCancelledAtAscCreatedAtAsc(
                ParticipationStatus.REFUND_PENDING,
                pageable
            )
        ).thenReturn(listOf(firstParticipation, secondParticipation))
        `when`(participationRepository.findByIdForUpdate(90L)).thenThrow(CustomException(ErrorCode.PARTICIPATION_NOT_FOUND))
        `when`(participationRepository.findByIdForUpdate(91L)).thenReturn(Optional.of(secondParticipation))
        `when`(paymentOrderRepository.findByUserIdAndGroupBuyIdForUpdate(2L, 10L)).thenReturn(secondOrder)
        `when`(paymentOrderRepository.findByOrderIdForUpdate(secondOrder.orderId)).thenReturn(secondOrder)
        `when`(paymentRepository.findByPaymentOrderOrderId(secondOrder.orderId)).thenReturn(secondPayment)
        `when`(portOnePaymentPort.cancelPayment("second-portone-payment-id", "MINIMUM_QUANTITY_NOT_MET", 6_000))
            .thenReturn(
                PortOnePaymentResult(
                    paymentId = "second-portone-payment-id",
                    status = "CANCELLED",
                    totalAmount = 6_000,
                    method = "CARD",
                    paidAt = secondOrder.approvedAt,
                    cancelledAt = refundedAt,
                )
            )

        val result = service.processPendingRefunds()

        assertEquals(2, result.targetCount)
        assertEquals(1, result.successCount)
        assertEquals(1, result.failedCount)
        assertEquals(ParticipationStatus.REFUNDED, secondParticipation.status)
        assertEquals(refundedAt, secondParticipation.refundedAt)
    }

    @Test
    fun `확정된 참여는 취소할 수 없다`() {
        val user = UserFixture.createKakaoUser(id = 1L)
        val groupBuy = createGroupBuy()
        val participation = Participation(
            user = user,
            groupBuy = groupBuy,
            quantity = 1,
            productAmount = 6000,
            feeAmount = 0,
            totalAmount = 6000,
            status = ParticipationStatus.CONFIRMED,
        ).apply { id = 78L }
        stubTransaction()
        `when`(participationRepository.findById(78L)).thenReturn(Optional.of(participation))
        `when`(participationRepository.findByIdForUpdate(78L)).thenReturn(Optional.of(participation))

        val ex = assertThrows<CustomException> {
            service.cancelParticipation(
                participationId = 78L,
                userId = 1L,
                request = CancelParticipationRequest(reason = ParticipationCancelReason.TIME_UNAVAILABLE),
            )
        }

        assertEquals(ErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED, ex.errorCode)
    }

    @Test
    fun `기타 취소 사유는 상세 내용이 필요하다`() {
        val ex = assertThrows<CustomException> {
            service.cancelParticipation(
                participationId = 77L,
                userId = 1L,
                request = CancelParticipationRequest(
                    reason = ParticipationCancelReason.OTHER,
                    reasonDetail = " ",
                ),
            )
        }

        assertEquals(ErrorCode.PARTICIPATION_CANCEL_REASON_DETAIL_REQUIRED, ex.errorCode)
    }

    private fun stubTransaction() {
        `when`(transactionManager.getTransaction(any(TransactionDefinition::class.java))).thenReturn(transactionStatus)
        lenient().`when`(redisLockUtil.lockKey(anyLong())).thenAnswer { "groupBuy:${it.arguments[0]}" }
        lenient().`when`(redisLockUtil.tryLockOrThrow(anyString(), anyLong(), anyLong())).thenReturn("lock-token")
        lenient().`when`(redisLockUtil.unlock(anyString(), anyString())).thenReturn(true)
    }

    private fun createOrderRequest(
        quantity: Int = 1,
        agreedNoCancelAfterGoal: Boolean = true,
        agreedRefundBeforeGoal: Boolean = true,
        agreedNoRefundAfterNoShow: Boolean = true,
        agreedNoWithdrawal: Boolean = true,
    ) = CreatePaymentOrderRequest(
        quantity = quantity,
        agreedNoCancelAfterGoal = agreedNoCancelAfterGoal,
        agreedRefundBeforeGoal = agreedRefundBeforeGoal,
        agreedNoRefundAfterNoShow = agreedNoRefundAfterNoShow,
        agreedNoWithdrawal = agreedNoWithdrawal,
    )

    private fun createPaymentOrder(
        user: User,
        groupBuy: GroupBuy,
        quantity: Int,
    ) = PaymentOrder(
        orderId = "MCJ-10-test",
        user = user,
        groupBuy = groupBuy,
        quantity = quantity,
        productAmount = 6000 * quantity,
        feeAmount = 0,
        totalAmount = 6000 * quantity,
        agreedNoCancelAfterGoal = true,
        agreedRefundBeforeGoal = true,
        agreedNoRefundAfterNoShow = true,
        agreedNoWithdrawal = true,
    )

    private fun createApprovedOrderAndPayment(user: User, groupBuy: GroupBuy): Pair<PaymentOrder, Payment> {
        val order = createPaymentOrder(user = user, groupBuy = groupBuy, quantity = 1).apply {
            approve(LocalDateTime.now().minusMinutes(5))
        }
        val payment = Payment(
            paymentOrder = order,
            pgPaymentId = order.orderId,
            orderId = order.orderId,
            amount = order.totalAmount,
            method = "CARD",
            approvedAt = order.approvedAt!!,
        )
        return order to payment
    }

    private fun stubCancelledWebhook(order: PaymentOrder, cancelledAt: LocalDateTime) {
        `when`(paymentOrderRepository.findByOrderId("MCJ-10-test")).thenReturn(order)
        `when`(paymentOrderRepository.findByOrderIdForUpdate("MCJ-10-test")).thenReturn(order)
        `when`(portOnePaymentPort.getPayment("MCJ-10-test"))
            .thenReturn(
                PortOnePaymentResult(
                    paymentId = "MCJ-10-test",
                    status = "CANCELLED",
                    totalAmount = 6000,
                    method = "CARD",
                    paidAt = order.approvedAt,
                    cancelledAt = cancelledAt,
                )
            )
    }

    private fun pendingRefundPageable(): Pageable =
        PageRequest.of(
            0,
            100,
            Sort.by(Sort.Order.asc("cancelledAt"), Sort.Order.asc("createdAt"), Sort.Order.asc("id"))
        )

    private fun createGroupBuy(
        id: Long = 10L,
        price: Int = 6000,
        currentQuantity: Int = 36,
        maxQuantity: Int = 100,
        status: GroupBuyStatus = GroupBuyStatus.IN_PROGRESS,
    ): GroupBuy {
        return GroupBuy(
            store = createStore(),
            groupBuyRequest = createGroupBuyRequest(),
            thumbnailKey = "https://example.com/image.jpg",
            productName = "두쫀쿠",
            productDescription = "설명",
            price = price,
            targetQuantity = 50,
            currentQuantity = currentQuantity,
            maxQuantity = maxQuantity,
            status = status,
            recruitmentStartAt = LocalDateTime.now(),
            deadline = LocalDateTime.now().plusDays(3),
            pickupDate = LocalDate.now().plusDays(5),
            pickupTimeStart = LocalTime.of(14, 0),
            pickupTimeEnd = LocalTime.of(18, 0),
            pickupLocation = "서울 성동구 성수동",
        ).apply { this.id = id }
    }

    private fun createStore(): Store =
        Store(
            name = "뭉치장 베이커리",
            address = "서울 성동구",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN
        ).apply { id = 1L }

    private fun createGroupBuyRequest(): GroupBuyRequest =
        GroupBuyRequest(
            user = com.moongchijang.support.UserFixture.createKakaoUser(id = 1L),
            storeName = "뭉치장 베이커리",
            storeAddress = "서울 성동구",
            productName = "두쫀쿠",
            desiredQuantity = 50,
            desiredPickupDate = LocalDate.now().plusDays(5)
        ).apply { id = 20L }

    companion object {
        private const val LOCK_WAIT_MS = 500L
        private const val LOCK_LEASE_MS = 10_000L
    }
}
