package com.moongchijang.domain.pickup.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.pickup.application.dto.NearestPickupQrReason
import com.moongchijang.domain.pickup.application.dto.PickupAvailabilityStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class PickupServiceTest {

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private val service: PickupService by lazy {
        PickupService(
            participationRepository = participationRepository,
            userRepository = userRepository,
        )
    }

    @Test
    fun `픽업 안내는 소비자 본인 참여의 매장 상품 픽업 정보를 반환한다`() {
        val participation = createParticipation(pickupDate = LocalDate.now().plusDays(1))
        `when`(participationRepository.findPickupDetailById(99L)).thenReturn(participation)

        val result = service.getPickupGuide(99L, 1L)

        assertEquals(99L, result.participationId)
        assertEquals(PickupAvailabilityStatus.LOCKED, result.availabilityStatus)
        assertEquals(PickupStatus.NOT_READY, result.pickupStatus)
        assertEquals("뭉치장 베이커리", result.storeName)
        assertEquals("서울 성동구 성수동", result.storeAddress)
        assertEquals("02-1234-5678", result.storePhone)
        assertEquals(37.54, result.latitude)
        assertEquals(127.05, result.longitude)
        assertNull(result.transitInfo)
        assertEquals("두쫀쿠", result.productName)
        assertEquals(2, result.quantity)
        assertEquals(participation.groupBuy.pickupDate, result.pickupDate)
        assertEquals(LocalTime.of(14, 0), result.pickupTimeStart)
        assertEquals(LocalTime.of(18, 0), result.pickupTimeEnd)
        assertEquals("매장 카운터", result.pickupLocation)
        assertNull(result.remainingMinutes)
    }

    @Test
    fun `픽업 안내는 매장 좌표가 없어도 주소와 null 좌표를 반환한다`() {
        val participation = createParticipation(
            pickupDate = LocalDate.now().plusDays(1),
            store = createStore(latitude = null, longitude = null),
        )
        `when`(participationRepository.findPickupDetailById(99L)).thenReturn(participation)

        val result = service.getPickupGuide(99L, 1L)

        assertEquals("서울 성동구 성수동", result.storeAddress)
        assertNull(result.latitude)
        assertNull(result.longitude)
    }

    @Test
    fun `본인 참여가 아니면 픽업 안내 조회를 거부한다`() {
        val participation = createParticipation(user = UserFixture.createKakaoUser(id = 2L))
        `when`(participationRepository.findPickupDetailById(99L)).thenReturn(participation)

        val ex = assertThrows<CustomException> {
            service.getPickupGuide(99L, 1L)
        }

        assertEquals(ErrorCode.PICKUP_PARTICIPATION_FORBIDDEN, ex.errorCode)
    }

    @Test
    fun `픽업일 전 QR 조회는 잠금 상태와 null QR을 반환한다`() {
        val participation = createParticipation(pickupDate = LocalDate.now().plusDays(1))
        `when`(participationRepository.findPickupDetailById(99L)).thenReturn(participation)

        val result = service.getPickupQr(99L, 1L)

        assertEquals(PickupAvailabilityStatus.LOCKED, result.availabilityStatus)
        assertEquals(PickupStatus.NOT_READY, result.pickupStatus)
        assertEquals("MCJ-P000099", result.reservationNumber)
        assertEquals("테스트유저", result.userName)
        assertEquals("두쫀쿠", result.productName)
        assertEquals(2, result.quantity)
        assertEquals("뭉치장 베이커리", result.storeName)
        assertEquals("서울 성동구 성수동", result.storeAddress)
        assertEquals("매장 카운터", result.pickupLocation)
        assertEquals(participation.groupBuy.pickupDate, result.pickupDate)
        assertEquals(LocalTime.of(14, 0), result.pickupTimeStart)
        assertEquals(LocalTime.of(18, 0), result.pickupTimeEnd)
        assertEquals(1, result.dDay)
        assertNull(result.qrCode)
        assertNull(participation.pickupToken)
    }

    @Test
    fun `픽업일 00시 이후 QR 토큰이 없으면 지연 생성하고 READY로 전환한다`() {
        val participation = createParticipation(pickupDate = LocalDate.now())
        `when`(participationRepository.findPickupDetailById(99L)).thenReturn(participation)
        `when`(participationRepository.existsByPickupToken(anyString())).thenReturn(false)

        val result = service.getPickupQr(99L, 1L)

        assertEquals(PickupAvailabilityStatus.AVAILABLE, result.availabilityStatus)
        assertEquals(PickupStatus.READY, result.pickupStatus)
        assertEquals(0, result.dDay)
        assertNotNull(result.qrCode)
        assertEquals(result.qrCode, participation.pickupToken)
    }

    @Test
    fun `이미 픽업 완료된 참여는 QR 토큰을 노출하지 않는다`() {
        val participation = createParticipation(
            pickupDate = LocalDate.now(),
            pickupStatus = PickupStatus.PICKED_UP,
            pickupToken = "used-token",
            pickedUpAt = LocalDateTime.now().minusHours(1),
        )
        `when`(participationRepository.findPickupDetailById(99L)).thenReturn(participation)

        val result = service.getPickupQr(99L, 1L)

        assertEquals(PickupAvailabilityStatus.PICKED_UP, result.availabilityStatus)
        assertEquals(PickupStatus.PICKED_UP, result.pickupStatus)
        assertNull(result.qrCode)
    }

    @Test
    fun `가장 가까운 QR은 당일 픽업 중 가장 이른 참여를 반환하고 다건 여부를 표시한다`() {
        val laterToday = createParticipation(id = 101L, pickupDate = LocalDate.now(), pickupTimeStart = LocalTime.of(16, 0))
        val earliestToday = createParticipation(id = 102L, pickupDate = LocalDate.now(), pickupTimeStart = LocalTime.of(10, 0))
        val future = createParticipation(id = 103L, pickupDate = LocalDate.now().plusDays(1), pickupTimeStart = LocalTime.of(9, 0))
        `when`(
            participationRepository.findNearestPickupQrCandidates(
                userId = 1L,
                status = ParticipationStatus.CONFIRMED,
                pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                fromDate = LocalDate.now(),
            )
        ).thenReturn(listOf(earliestToday, laterToday, future))
        `when`(participationRepository.existsByPickupToken(anyString())).thenReturn(false)

        val result = service.getNearestPickupQr(1L)

        assertEquals(true, result.hasCandidate)
        assertEquals(true, result.hasMultipleToday)
        assertNull(result.reason)
        assertEquals(102L, result.item?.participationId)
        assertEquals("MCJ-P000102", result.item?.reservationNumber)
        assertEquals(PickupAvailabilityStatus.AVAILABLE, result.item?.availabilityStatus)
        assertEquals(0, result.item?.dDay)
        assertEquals("서울 성동구 성수동", result.item?.storeAddress)
        assertNotNull(result.item?.qrCode)
        assertEquals(PickupStatus.READY, earliestToday.pickupStatus)
    }

    @Test
    fun `당일 픽업이 없고 미래 픽업만 있으면 잠금 상태 후보를 반환한다`() {
        val future = createParticipation(id = 103L, pickupDate = LocalDate.now().plusDays(1))
        `when`(
            participationRepository.findNearestPickupQrCandidates(
                userId = 1L,
                status = ParticipationStatus.CONFIRMED,
                pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                fromDate = LocalDate.now(),
            )
        ).thenReturn(listOf(future))

        val result = service.getNearestPickupQr(1L)

        assertEquals(true, result.hasCandidate)
        assertEquals(false, result.hasMultipleToday)
        assertEquals(NearestPickupQrReason.ONLY_FUTURE_PICKUP, result.reason)
        assertEquals(103L, result.item?.participationId)
        assertEquals(PickupAvailabilityStatus.LOCKED, result.item?.availabilityStatus)
        assertEquals(1, result.item?.dDay)
        assertNull(result.item?.qrCode)
    }

    @Test
    fun `픽업일이 지난 QR 조회는 음수 D-day를 반환한다`() {
        val participation = createParticipation(
            pickupDate = LocalDate.now().minusDays(1),
            pickupStatus = PickupStatus.PICKED_UP,
            pickedUpAt = LocalDateTime.now().minusHours(1),
        )
        `when`(participationRepository.findPickupDetailById(99L)).thenReturn(participation)

        val result = service.getPickupQr(99L, 1L)

        assertEquals(PickupAvailabilityStatus.PICKED_UP, result.availabilityStatus)
        assertEquals(-1, result.dDay)
    }

    @Test
    fun `가장 가까운 QR 후보가 없으면 빈 상태를 반환한다`() {
        `when`(
            participationRepository.findNearestPickupQrCandidates(
                userId = 1L,
                status = ParticipationStatus.CONFIRMED,
                pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                fromDate = LocalDate.now(),
            )
        ).thenReturn(emptyList())

        val result = service.getNearestPickupQr(1L)

        assertEquals(false, result.hasCandidate)
        assertEquals(false, result.hasMultipleToday)
        assertEquals(NearestPickupQrReason.NO_AVAILABLE_PICKUP, result.reason)
        assertNull(result.item)
    }

    @Test
    fun `픽업일 전 QR 검증은 실패한다`() {
        val participation = createParticipation(
            pickupDate = LocalDate.now().plusDays(1),
            pickupStatus = PickupStatus.READY,
            pickupToken = "qr-token",
        )
        `when`(participationRepository.findByPickupTokenForUpdate("qr-token")).thenReturn(participation)

        val ex = assertThrows<CustomException> {
            service.verifyPickup("qr-token", 7L)
        }

        assertEquals(ErrorCode.PICKUP_LOCKED, ex.errorCode)
        verify(userRepository, never()).findByIdAndDeletedAtIsNull(7L)
    }

    @Test
    fun `QR 검증 처리자를 찾을 수 없으면 실패한다`() {
        val participation = createParticipation(
            pickupDate = LocalDate.now(),
            pickupStatus = PickupStatus.READY,
            pickupToken = "qr-token",
        )
        `when`(participationRepository.findByPickupTokenForUpdate("qr-token")).thenReturn(participation)
        `when`(userRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(null)

        val ex = assertThrows<CustomException> {
            service.verifyPickup("qr-token", 7L)
        }

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.errorCode)
    }

    @Test
    fun `QR 검증 성공은 픽업 완료 처리하고 처리자를 저장한다`() {
        val processor = UserFixture.createKakaoUser(id = 7L)
        val participation = createParticipation(
            pickupDate = LocalDate.now(),
            pickupStatus = PickupStatus.READY,
            pickupToken = "qr-token",
        )
        `when`(participationRepository.findByPickupTokenForUpdate("qr-token")).thenReturn(participation)
        `when`(userRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(processor)

        val result = service.verifyPickup("qr-token", 7L)

        assertEquals(PickupStatus.PICKED_UP, result.pickupStatus)
        assertNotNull(result.pickedUpAt)
        assertEquals(7L, result.pickupProcessedByUserId)
        assertEquals(PickupStatus.PICKED_UP, participation.pickupStatus)
        assertNotNull(participation.pickedUpAt)
        assertEquals(processor, participation.pickupProcessedBy)
    }

    @Test
    fun `이미 사용된 QR은 재사용을 거부한다`() {
        val participation = createParticipation(
            pickupDate = LocalDate.now(),
            pickupStatus = PickupStatus.PICKED_UP,
            pickupToken = "qr-token",
            pickedUpAt = LocalDateTime.now().minusMinutes(10),
        )
        `when`(participationRepository.findByPickupTokenForUpdate("qr-token")).thenReturn(participation)

        val ex = assertThrows<CustomException> {
            service.verifyPickup("qr-token", 7L)
        }

        assertEquals(ErrorCode.PICKUP_ALREADY_USED, ex.errorCode)
        verify(userRepository, never()).findByIdAndDeletedAtIsNull(7L)
    }

    private fun createParticipation(
        id: Long = 99L,
        user: User = UserFixture.createKakaoUser(id = 1L),
        pickupDate: LocalDate = LocalDate.now(),
        pickupTimeStart: LocalTime = LocalTime.of(14, 0),
        pickupStatus: PickupStatus = PickupStatus.NOT_READY,
        pickupToken: String? = null,
        pickedUpAt: LocalDateTime? = null,
        store: Store = createStore(),
    ): Participation =
        Participation(
            user = user,
            groupBuy = createGroupBuy(
                pickupDate = pickupDate,
                pickupTimeStart = pickupTimeStart,
                store = store,
            ),
            quantity = 2,
            productAmount = 12_000,
            feeAmount = 0,
            totalAmount = 12_000,
            status = ParticipationStatus.CONFIRMED,
            pickupStatus = pickupStatus,
            pickupToken = pickupToken,
            pickedUpAt = pickedUpAt,
        ).apply { this.id = id }

    private fun createGroupBuy(
        pickupDate: LocalDate,
        pickupTimeStart: LocalTime = LocalTime.of(14, 0),
        store: Store = createStore(),
    ): GroupBuy =
        GroupBuy(
            store = store,
            groupBuyRequest = createGroupBuyRequest(pickupDate),
            thumbnailUrl = "https://example.com/image.jpg",
            productName = "두쫀쿠",
            productDescription = "설명",
            price = 6000,
            targetQuantity = 50,
            currentQuantity = 50,
            maxQuantity = 100,
            status = GroupBuyStatus.ACHIEVED,
            deadline = LocalDateTime.now().minusDays(1),
            pickupDate = pickupDate,
            pickupTimeStart = pickupTimeStart,
            pickupTimeEnd = LocalTime.of(18, 0),
            pickupLocation = "매장 카운터",
        ).apply { id = 10L }

    private fun createStore(
        latitude: Double? = 37.54,
        longitude: Double? = 127.05,
    ): Store =
        Store(
            name = "뭉치장 베이커리",
            address = "서울 성동구 성수동",
            phoneNumber = "02-1234-5678",
            latitude = latitude,
            longitude = longitude,
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_SEONGSU_GEONDAE_GWANGJIN,
        ).apply { id = 20L }

    private fun createGroupBuyRequest(pickupDate: LocalDate): GroupBuyRequest =
        GroupBuyRequest(
            userId = 1L,
            storeName = "뭉치장 베이커리",
            storeAddress = "서울 성동구 성수동",
            productName = "두쫀쿠",
            desiredQuantity = 50,
            desiredPickupDate = pickupDate,
        ).apply { id = 30L }
}
