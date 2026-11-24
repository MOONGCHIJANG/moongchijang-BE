package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipantRole
import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipationUpsertRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

class TogetherBuyParticipationCommandServiceTest {
    private val groupBuyRepository: GroupBuyRepository = mock(GroupBuyRepository::class.java)
    private val participationRepository: ParticipationRepository = mock(ParticipationRepository::class.java)
    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-06-01T01:00:00Z"), ZoneOffset.UTC)
    private val service = TogetherBuyParticipationCommandService(
        groupBuyRepository,
        participationRepository,
        userRepository,
        clock,
    )

    @Test
    fun `참여자가 모집 중인 방에 수량을 담으면 참여와 방 합계를 생성한다`() {
        val user = UserFixture.createKakaoUser(id = 2L)
        val room = room(currentQuantity = 2)
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))
        `when`(userRepository.findByIdAndDeletedAtIsNull(user.id!!)).thenReturn(user)
        `when`(participationRepository.findByUserIdAndGroupBuyId(user.id!!, room.id)).thenReturn(null)
        `when`(participationRepository.save(any(Participation::class.java))).thenAnswer { invocation ->
            (invocation.arguments[0] as Participation).apply { id = 7L }
        }

        val response = service.upsert(room.id, user.id!!, TogetherBuyParticipationUpsertRequest(quantity = 4))

        assertThat(response.participationId).isEqualTo(7L)
        assertThat(response.role).isEqualTo(TogetherBuyParticipantRole.PARTICIPANT)
        assertThat(response.quantity).isEqualTo(4)
        assertThat(response.productAmount).isEqualTo(24_000)
        assertThat(response.quantityAdjusted).isFalse()
        assertThat(room.currentQuantity).isEqualTo(6)
        val captor = ArgumentCaptor.forClass(Participation::class.java)
        verify(participationRepository).save(captor.capture())
        assertThat(captor.value.user).isSameAs(user)
        assertThat(captor.value.totalAmount).isEqualTo(24_000)
    }

    @Test
    fun `기존 참여자는 본인 기존 수량을 포함한 잔여 수량만큼 자동 조정된다`() {
        val user = UserFixture.createKakaoUser(id = 2L)
        val room = room(currentQuantity = 9, maxQuantity = 10)
        val participation = participation(room, user, quantity = 3)
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))
        `when`(userRepository.findByIdAndDeletedAtIsNull(user.id!!)).thenReturn(user)
        `when`(participationRepository.findByUserIdAndGroupBuyId(user.id!!, room.id)).thenReturn(participation)
        `when`(participationRepository.save(participation)).thenReturn(participation)

        val response = service.upsert(room.id, user.id!!, TogetherBuyParticipationUpsertRequest(quantity = 8))

        assertThat(response.quantity).isEqualTo(4)
        assertThat(response.quantityAdjusted).isTrue()
        assertThat(room.currentQuantity).isEqualTo(10)
        assertThat(participation.productAmount).isEqualTo(24_000)
    }

    @Test
    fun `참여 취소는 참여를 삭제하고 방 합계에서 해당 수량을 뺀다`() {
        val user = UserFixture.createKakaoUser(id = 2L)
        val room = room(currentQuantity = 6)
        val participation = participation(room, user, quantity = 2)
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))
        `when`(participationRepository.findByUserIdAndGroupBuyId(user.id!!, room.id)).thenReturn(participation)

        service.cancel(room.id, user.id!!)

        assertThat(room.currentQuantity).isEqualTo(4)
        verify(participationRepository).delete(participation)
    }

    @Test
    fun `총대는 참여 취소 API로 방을 나갈 수 없다`() {
        val room = room(currentQuantity = 2)
        val organizer = requireNotNull(room.organizer)
        val participation = participation(room, organizer, quantity = 2)
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))
        `when`(participationRepository.findByUserIdAndGroupBuyId(organizer.id!!, room.id)).thenReturn(participation)

        val exception = assertThrows<CustomException> {
            service.cancel(room.id, organizer.id!!)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED)
        assertThat(room.currentQuantity).isEqualTo(2)
        verify(participationRepository, never()).delete(participation)
    }

    @Test
    fun `모집 마감된 방에는 참여 수량을 변경할 수 없다`() {
        val user = UserFixture.createKakaoUser(id = 2L)
        val room = room()
        room.deadline = LocalDateTime.of(2026, 5, 31, 0, 0)
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))

        val exception = assertThrows<CustomException> {
            service.upsert(room.id, user.id!!, TogetherBuyParticipationUpsertRequest(quantity = 1))
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.GROUPBUY_DEADLINE_PASSED)
        verify(participationRepository, never()).save(any(Participation::class.java))
    }

    @Test
    fun `최종 확약 요청 중인 방에서는 참여 수량을 변경할 수 없다`() {
        val user = UserFixture.createKakaoUser(id = 2L)
        val room = room().apply { confirmationDeadline = LocalDateTime.of(2026, 6, 1, 3, 0) }
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))

        val exception = assertThrows<CustomException> {
            service.upsert(room.id, user.id!!, TogetherBuyParticipationUpsertRequest(quantity = 1))
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.GROUPBUY_CONFIRMATION_IN_PROGRESS)
        verify(participationRepository, never()).save(any(Participation::class.java))
    }

    @Test
    fun `단독 구매 방에는 총대 외 참여자를 추가할 수 없다`() {
        val user = UserFixture.createKakaoUser(id = 2L)
        val room = room().apply { isSoloRoom = true }
        `when`(groupBuyRepository.findWithLockById(room.id)).thenReturn(Optional.of(room))
        `when`(userRepository.findByIdAndDeletedAtIsNull(user.id!!)).thenReturn(user)
        `when`(participationRepository.findByUserIdAndGroupBuyId(user.id!!, room.id)).thenReturn(null)

        val exception = assertThrows<CustomException> {
            service.upsert(room.id, user.id!!, TogetherBuyParticipationUpsertRequest(quantity = 1))
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.GROUPBUY_NOT_RECRUITING)
    }

    private fun room(
        currentQuantity: Int = 2,
        maxQuantity: Int = 10,
    ): GroupBuy = GroupBuyFixture.createGroupBuy(
        id = 10L,
        status = GroupBuyStatus.IN_PROGRESS,
        currentQuantity = currentQuantity,
        maxQuantity = maxQuantity,
    ).apply {
        organizer = UserFixture.createKakaoUser(id = 1L)
    }

    private fun participation(room: GroupBuy, user: com.moongchijang.domain.user.domain.entity.User, quantity: Int) =
        Participation(
            user = user,
            groupBuy = room,
            quantity = quantity,
            productAmount = room.price * quantity,
            feeAmount = 0,
            totalAmount = room.price * quantity,
        ).apply { id = 20L }
}
