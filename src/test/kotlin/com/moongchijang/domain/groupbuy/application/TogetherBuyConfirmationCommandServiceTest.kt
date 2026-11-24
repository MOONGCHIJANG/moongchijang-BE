package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyConfirmationRequest
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyConfirmationRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationConfirmation
import com.moongchijang.domain.participation.domain.entity.ParticipationConfirmationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationConfirmationRepository
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

class TogetherBuyConfirmationCommandServiceTest {
    private val groupBuyRepository: GroupBuyRepository = mock(GroupBuyRepository::class.java)
    private val participationRepository: ParticipationRepository = mock(ParticipationRepository::class.java)
    private val participationConfirmationRepository: ParticipationConfirmationRepository = mock(ParticipationConfirmationRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-06-01T01:00:00Z"), ZoneOffset.UTC)
    private val service = TogetherBuyConfirmationCommandService(
        groupBuyRepository,
        participationRepository,
        participationConfirmationRepository,
        clock,
    )

    @Test
    fun `총대는 최소 수량을 충족한 방에 최종 확약을 요청하고 활성 참여자를 대기 상태로 전환한다`() {
        val room = room(currentQuantity = 5)
        val organizer = requireNotNull(room.organizer)
        val participant = UserFixture.createKakaoUser(id = 2L)
        val participations = listOf(participation(room, organizer, 1, 10L), participation(room, participant, 4, 20L))
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))
        `when`(participationRepository.findAllActiveWithUserByGroupBuyId(room.id)).thenReturn(participations)

        val response = service.request(room.id, organizer.id!!, null)

        assertThat(response.status).isEqualTo(TogetherBuyConfirmationRequestStatus.REQUESTED)
        assertThat(response.confirmationDeadline).isEqualTo(LocalDateTime.of(2026, 6, 1, 11, 0))
        assertThat(response.confirmedParticipantCount).isZero()
        assertThat(response.pendingParticipantCount).isEqualTo(2)
        assertThat(room.confirmationDeadline).isEqualTo(response.confirmationDeadline)
        assertThat(participations).allSatisfy {
            assertThat(it.confirmationStatus).isEqualTo(ParticipationConfirmationStatus.REQUESTED)
        }
        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Iterable::class.java) as ArgumentCaptor<Iterable<ParticipationConfirmation>>
        org.mockito.Mockito.verify(participationConfirmationRepository).saveAll(captor.capture())
        val histories = captor.value.toList()
        assertThat(histories).hasSize(2)
        assertThat(histories).allSatisfy {
            assertThat(it.requestedAt).isEqualTo(LocalDateTime.of(2026, 6, 1, 10, 0))
            assertThat(it.deadline).isEqualTo(response.confirmationDeadline)
        }
    }

    @Test
    fun `총대가 아닌 사용자는 최종 확약을 요청할 수 없다`() {
        val room = room(currentQuantity = 5)
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))

        val exception = assertThrows<CustomException> {
            service.request(room.id, 2L, null)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.FORBIDDEN)
    }

    @Test
    fun `최소 수량을 충족하지 못한 방은 최종 확약을 요청할 수 없다`() {
        val room = room(currentQuantity = 4)
        val organizer = requireNotNull(room.organizer)
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))

        val exception = assertThrows<CustomException> {
            service.request(room.id, organizer.id!!, null)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.GROUPBUY_CONFIRMATION_NOT_AVAILABLE)
    }

    @Test
    fun `모집 마감 이후 또는 모집 마감보다 늦은 확약 마감은 요청할 수 없다`() {
        val room = room(currentQuantity = 5)
        val organizer = requireNotNull(room.organizer)
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))

        val exception = assertThrows<CustomException> {
            service.request(
                room.id,
                organizer.id!!,
                TogetherBuyConfirmationRequest(LocalDateTime.of(2026, 6, 2, 10, 1)),
            )
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.GROUPBUY_CONFIRMATION_DEADLINE_INVALID)
    }

    @Test
    fun `이미 최종 확약을 요청한 방은 다시 요청할 수 없다`() {
        val room = room(currentQuantity = 5).apply { confirmationDeadline = LocalDateTime.of(2026, 6, 1, 11, 0) }
        val organizer = requireNotNull(room.organizer)
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))

        val exception = assertThrows<CustomException> {
            service.request(room.id, organizer.id!!, null)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.GROUPBUY_CONFIRMATION_ALREADY_REQUESTED)
    }

    private fun room(currentQuantity: Int): GroupBuy = GroupBuyFixture.createGroupBuy(
        id = 10L,
        status = GroupBuyStatus.IN_PROGRESS,
        deadline = LocalDateTime.of(2026, 6, 2, 10, 0),
        targetQuantity = 5,
        currentQuantity = currentQuantity,
        maxQuantity = 10,
    ).apply {
        minQuantity = 5
        organizer = UserFixture.createKakaoUser(id = 1L)
    }

    private fun participation(room: GroupBuy, user: com.moongchijang.domain.user.domain.entity.User, quantity: Int, id: Long) =
        Participation(
            user = user,
            groupBuy = room,
            quantity = quantity,
            productAmount = room.price * quantity,
            feeAmount = 0,
            totalAmount = room.price * quantity,
        ).apply { this.id = id }
}
