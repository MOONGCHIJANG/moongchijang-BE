package com.moongchijang.domain.home.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.RoomPickupRepository
import com.moongchijang.domain.groupbuy.domain.repository.SettlementTransferRepository
import com.moongchijang.domain.home.application.dto.HomeTogetherRoomAction
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import com.moongchijang.domain.store.domain.entity.Store
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
    @Mock private lateinit var groupBuyRepository: GroupBuyRepository
    @Mock private lateinit var participationRepository: ParticipationRepository
    @Mock private lateinit var settlementTransferRepository: SettlementTransferRepository
    @Mock private lateinit var roomPickupRepository: RoomPickupRepository

    private val clock = Clock.fixed(Instant.parse("2026-10-26T01:00:00Z"), ZoneOffset.UTC)
    private lateinit var service: HomeTogetherStatusQueryService

    @BeforeEach
    fun setUp() {
        service = HomeTogetherStatusQueryService(
            groupBuyRepository,
            participationRepository,
            settlementTransferRepository,
            roomPickupRepository,
            clock,
        )
    }

    @Test
    fun `오늘 픽업 총대에게만 QR 열기와 탭 뱃지를 제공하고 방 카드에서는 제외한다`() {
        val room = room(id = 10L, paidAt = LocalDateTime.of(2026, 10, 25, 12, 0), pickupDate = LocalDate.of(2026, 10, 26), pickupTimeEnd = LocalTime.of(12, 0))
        stubRooms(room)
        `when`(roomPickupRepository.findActiveGroupBuyIds(setOf(10L), LocalDateTime.of(2026, 10, 26, 10, 0)))
            .thenReturn(setOf(10L))

        val result = service.getStatus(1L)

        assertThat(result.isTodayPickupLeader).isTrue()
        assertThat(result.togetherTabBadgeCount).isEqualTo(1)
        assertThat(result.todayPickups.single().canOpenQr).isTrue()
        assertThat(result.rooms).isEmpty()
    }

    @Test
    fun `최소 수량을 채운 방은 총대 결제 행동으로 노출한다`() {
        val room = room(id = 11L, pickupDate = LocalDate.of(2026, 10, 27))
        stubRooms(room)

        val result = service.getStatus(1L)

        assertThat(result.togetherTabBadgeCount).isEqualTo(1)
        assertThat(result.rooms.single().nextAction).isEqualTo(HomeTogetherRoomAction.PAY)
    }

    @Test
    fun `비로그인 사용자에게 첫 방문 안내를 제공한다`() {
        val result = service.getStatus(null)

        assertThat(result.showFirstVisitGuide).isTrue()
    }

    @Test
    fun `개설과 참여 이력이 없는 사용자에게만 첫 방문 안내를 제공한다`() {
        `when`(participationRepository.findAllActiveWithGroupBuyByUserId(2L)).thenReturn(emptyList())
        `when`(groupBuyRepository.findActiveByOrganizerId(2L, setOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED)))
            .thenReturn(emptyList())
        `when`(groupBuyRepository.existsByOrganizerId(2L)).thenReturn(false)
        `when`(participationRepository.existsByUserId(2L)).thenReturn(false)

        val result = service.getStatus(2L)

        assertThat(result.showFirstVisitGuide).isTrue()
    }

    private fun stubRooms(room: GroupBuy) {
        `when`(participationRepository.findAllActiveWithGroupBuyByUserId(1L)).thenReturn(emptyList())
        `when`(groupBuyRepository.findActiveByOrganizerId(1L, setOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED)))
            .thenReturn(listOf(room))
        `when`(settlementTransferRepository.findGroupBuyIdsByParticipantIdAndStatus(1L)).thenReturn(emptySet())
    }

    private fun room(
        id: Long,
        pickupDate: LocalDate,
        pickupTimeEnd: LocalTime = LocalTime.of(11, 0),
        paidAt: LocalDateTime? = null,
    ): GroupBuy = GroupBuy(
        store = Store("테스트 매장", "서울", region = RegionType.SEOUL, district = DistrictType.SEOUL_GANGNAM_YEOKSAM_SAMSEONG, id = 1L),
        organizer = User(provider = AuthProvider.EMAIL, id = 1L),
        productName = "테스트 상품",
        productDescription = "설명",
        price = 1_000,
        targetQuantity = 10,
        minQuantity = 10,
        currentQuantity = 10,
        maxQuantity = 20,
        status = GroupBuyStatus.IN_PROGRESS,
        recruitmentStartAt = LocalDateTime.of(2026, 10, 20, 0, 0),
        deadline = LocalDateTime.of(2026, 10, 25, 23, 59),
        pickupDate = pickupDate,
        pickupTimeStart = LocalTime.of(10, 0),
        pickupTimeEnd = pickupTimeEnd,
        pickupLocation = "테스트 매장",
        paidAt = paidAt,
        id = id,
    )
}
