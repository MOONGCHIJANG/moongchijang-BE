package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.TogetherBuyParticipantRole
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

class TogetherBuyParticipantRosterQueryServiceTest {
    private val groupBuyRepository: GroupBuyRepository = mock(GroupBuyRepository::class.java)
    private val participationRepository: ParticipationRepository = mock(ParticipationRepository::class.java)
    private val service = TogetherBuyParticipantRosterQueryService(groupBuyRepository, participationRepository)

    @Test
    fun `참여자는 총대가 첫 번째인 방 참여자 목록을 조회한다`() {
        val room = room()
        val organizer = requireNotNull(room.organizer)
        val participant = UserFixture.createKakaoUser(id = 2L)
        val organizerParticipation = participation(room, organizer, quantity = 2, id = 10L)
        val participantParticipation = participation(room, participant, quantity = 3, id = 20L)
        `when`(groupBuyRepository.findWithStoreById(room.id)).thenReturn(Optional.of(room))
        `when`(participationRepository.findAllActiveWithUserByGroupBuyId(room.id))
            .thenReturn(listOf(participantParticipation, organizerParticipation))

        val response = service.getRoster(room.id, participant.id!!)

        assertThat(response.organizerParticipationId).isEqualTo(organizerParticipation.id)
        assertThat(response.participants).extracting("participationId")
            .containsExactly(organizerParticipation.id, participantParticipation.id)
        assertThat(response.participants.first().role).isEqualTo(TogetherBuyParticipantRole.ORGANIZER)
        assertThat(response.participants.first().quantityAdjusted).isFalse()
    }

    @Test
    fun `총대는 본인 방의 참여자 목록을 조회한다`() {
        val room = room()
        val organizer = requireNotNull(room.organizer)
        val organizerParticipation = participation(room, organizer, quantity = 2, id = 10L)
        `when`(groupBuyRepository.findWithStoreById(room.id)).thenReturn(Optional.of(room))
        `when`(participationRepository.findAllActiveWithUserByGroupBuyId(room.id))
            .thenReturn(listOf(organizerParticipation))

        val response = service.getRoster(room.id, organizer.id!!)

        assertThat(response.participants).hasSize(1)
        assertThat(response.participants.single().role).isEqualTo(TogetherBuyParticipantRole.ORGANIZER)
    }

    @Test
    fun `방에 참여하지 않은 사용자는 참여자 목록을 조회할 수 없다`() {
        val room = room()
        val organizer = requireNotNull(room.organizer)
        val organizerParticipation = participation(room, organizer, quantity = 2, id = 10L)
        `when`(groupBuyRepository.findWithStoreById(room.id)).thenReturn(Optional.of(room))
        `when`(participationRepository.findAllActiveWithUserByGroupBuyId(room.id))
            .thenReturn(listOf(organizerParticipation))

        val exception = assertThrows<CustomException> {
            service.getRoster(room.id, 99L)
        }

        assertThat(exception.errorCode).isEqualTo(ErrorCode.FORBIDDEN)
    }

    private fun room(): GroupBuy = GroupBuyFixture.createGroupBuy(
        id = 10L,
        status = GroupBuyStatus.IN_PROGRESS,
    ).apply {
        organizer = UserFixture.createKakaoUser(id = 1L)
    }

    private fun participation(room: GroupBuy, user: User, quantity: Int, id: Long) = Participation(
        user = user,
        groupBuy = room,
        quantity = quantity,
        productAmount = room.price * quantity,
        feeAmount = 0,
        totalAmount = room.price * quantity,
    ).apply { this.id = id }
}
