package com.moongchijang.domain.home.application

import com.moongchijang.domain.home.application.dto.HomeTogetherRoomAction
import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStorageType
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.together.domain.entity.TogetherRoom
import com.moongchijang.domain.together.domain.entity.TogetherRoomStatus
import com.moongchijang.domain.together.domain.repository.ParticipantSettlementRepository
import com.moongchijang.domain.together.domain.repository.PickupQrCodeRepository
import com.moongchijang.domain.together.domain.repository.TogetherRoomMemberRepository
import com.moongchijang.domain.together.domain.repository.TogetherRoomRepository
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
class HomeTogetherStatusQueryServiceTest {

    @Mock
    private lateinit var togetherRoomRepository: TogetherRoomRepository

    @Mock
    private lateinit var togetherRoomMemberRepository: TogetherRoomMemberRepository

    @Mock
    private lateinit var participantSettlementRepository: ParticipantSettlementRepository

    @Mock
    private lateinit var pickupQrCodeRepository: PickupQrCodeRepository

    private val clock = Clock.fixed(Instant.parse("2026-10-26T01:00:00Z"), ZoneOffset.UTC)

    private lateinit var service: HomeTogetherStatusQueryService

    @BeforeEach
    fun setUp() {
        service = HomeTogetherStatusQueryService(
            togetherRoomRepository = togetherRoomRepository,
            togetherRoomMemberRepository = togetherRoomMemberRepository,
            participantSettlementRepository = participantSettlementRepository,
            pickupQrCodeRepository = pickupQrCodeRepository,
            clock = clock,
        )
    }

    @Test
    fun `오늘 픽업 총대에게만 QR 열기와 탭 뱃지를 제공하고 방 카드에서는 제외한다`() {
        val room = room(
            id = 10L,
            status = TogetherRoomStatus.PICKUP_READY,
            pickupDate = LocalDate.of(2026, 10, 26),
            pickupTimeEnd = LocalTime.of(12, 0),
        )
        `when`(togetherRoomMemberRepository.findAllActiveWithRoomByUserId(1L)).thenReturn(emptyList())
        `when`(
            togetherRoomRepository.findActiveByLeaderId(
                1L,
                setOf(TogetherRoomStatus.PICKUP_COMPLETED, TogetherRoomStatus.CLOSED, TogetherRoomStatus.CANCELLED),
            )
        ).thenReturn(listOf(room))
        `when`(participantSettlementRepository.findRoomIdsByParticipantIdAndStatus(1L)).thenReturn(emptySet())
        `when`(pickupQrCodeRepository.findActiveRoomIds(setOf(10L), LocalDateTime.of(2026, 10, 26, 10, 0)))
            .thenReturn(setOf(10L))

        val result = service.getStatus(1L)

        assertThat(result.isTodayPickupLeader).isTrue()
        assertThat(result.togetherTabBadgeCount).isEqualTo(1)
        assertThat(result.todayPickups.single().canOpenQr).isTrue()
        assertThat(result.rooms).isEmpty()
        assertThat(result.showTogetherRoomModule).isFalse()
    }

    @Test
    fun `최소 수량을 채운 모집 중 방은 총대 결제 행동으로 우선 노출한다`() {
        val room = room(id = 11L, status = TogetherRoomStatus.RECRUITING, pickupDate = LocalDate.of(2026, 10, 27))
        `when`(togetherRoomMemberRepository.findAllActiveWithRoomByUserId(1L)).thenReturn(emptyList())
        `when`(
            togetherRoomRepository.findActiveByLeaderId(
                1L,
                setOf(TogetherRoomStatus.PICKUP_COMPLETED, TogetherRoomStatus.CLOSED, TogetherRoomStatus.CANCELLED),
            )
        ).thenReturn(listOf(room))
        `when`(participantSettlementRepository.findRoomIdsByParticipantIdAndStatus(1L)).thenReturn(emptySet())

        val result = service.getStatus(1L)

        assertThat(result.togetherTabBadgeCount).isEqualTo(1)
        assertThat(result.rooms.single().nextAction).isEqualTo(HomeTogetherRoomAction.PAY)
    }

    private fun room(
        id: Long,
        status: TogetherRoomStatus,
        pickupDate: LocalDate,
        pickupTimeEnd: LocalTime = LocalTime.of(11, 0),
    ): TogetherRoom {
        val store = Store(
            name = "테스트 매장",
            address = "서울",
            region = RegionType.SEOUL,
            district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG,
            id = 1L,
        )
        val product = Product(
            store = store,
            name = "테스트 상품",
            description = "설명",
            unitPrice = 1_000,
            minQuantity = 10,
            maxQuantity = 20,
            preparationDays = 2,
            storageType = ProductStorageType.ROOM_TEMPERATURE,
            thumbnailKey = "test.jpg",
            id = 2L,
        )
        return TogetherRoom(
            product = product,
            store = store,
            leader = User(provider = AuthProvider.EMAIL, id = 1L),
            status = status,
            pickupDate = pickupDate,
            pickupTimeStart = LocalTime.of(10, 0),
            pickupTimeEnd = pickupTimeEnd,
            paymentDeadlineAt = LocalDateTime.of(2026, 10, 25, 23, 59),
            currentQuantity = 10,
            totalAmount = 10_000,
            id = id,
        )
    }
}
