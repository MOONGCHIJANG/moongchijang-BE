package com.moongchijang.domain.participation.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.infrastructure.lock.RedisLockUtil
import com.moongchijang.domain.participation.application.dto.ParticipationCreateRequest
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.ParticipationFixture.createGroupBuy
import com.moongchijang.support.ParticipationFixture.createUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ParticipationServiceTest {

    @Mock
    private lateinit var redisLockUtil: RedisLockUtil

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @Mock
    private lateinit var participationRepository: ParticipationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var service: ParticipationService

    @Test
    fun `이미 참여한 공구면 GROUPBUY_ALREADY_PARTICIPATED 예외 발생`() {
        val userId = 1L
        val groupBuyId = 10L
        val request = ParticipationCreateRequest(quantity = 1)

        `when`(participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(true)

        val ex = assertThrows<CustomException> {
            service.createParticipation(userId, groupBuyId, request)
        }

        assertEquals(ErrorCode.GROUPBUY_ALREADY_PARTICIPATED, ex.errorCode)
        verify(redisLockUtil, never()).lockKey(groupBuyId)
    }

    @Test
    fun `모집중 상태가 아니면 GROUPBUY_NOT_RECRUITING 예외 발생`() {
        val userId = 1L
        val groupBuyId = 11L
        val request = ParticipationCreateRequest(quantity = 1)
        val key = "groupBuy:$groupBuyId"
        val token = "token-1"
        val user = createUser(userId)
        val closedGroupBuy = createGroupBuy(id = groupBuyId, status = GroupBuyStatus.CLOSED)

        stubBase(userId, groupBuyId, key, token, user, closedGroupBuy)

        val ex = assertThrows<CustomException> {
            service.createParticipation(userId, groupBuyId, request)
        }

        assertEquals(ErrorCode.GROUPBUY_NOT_RECRUITING, ex.errorCode)
        verify(redisLockUtil).unlock(key, token)
    }

    @Test
    fun `마감된 공구면 GROUPBUY_DEADLINE_PASSED 예외 발생`() {
        val userId = 1L
        val groupBuyId = 12L
        val request = ParticipationCreateRequest(quantity = 1)
        val key = "groupBuy:$groupBuyId"
        val token = "token-2"
        val user = createUser(userId)
        val expiredGroupBuy = createGroupBuy(
            id = groupBuyId,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = LocalDateTime.now().minusMinutes(1)
        )

        stubBase(userId, groupBuyId, key, token, user, expiredGroupBuy)

        val ex = assertThrows<CustomException> {
            service.createParticipation(userId, groupBuyId, request)
        }

        assertEquals(ErrorCode.GROUPBUY_DEADLINE_PASSED, ex.errorCode)
        verify(redisLockUtil).unlock(key, token)
    }

    @Test
    fun `조건부 차감 실패면 GROUPBUY_SOLD_OUT 예외 발생`() {
        val userId = 1L
        val groupBuyId = 13L
        val request = ParticipationCreateRequest(quantity = 2)
        val key = "groupBuy:$groupBuyId"
        val token = "token-3"
        val user = createUser(userId)
        val groupBuy = createGroupBuy(id = groupBuyId, status = GroupBuyStatus.IN_PROGRESS)

        stubBase(userId, groupBuyId, key, token, user, groupBuy)
        `when`(groupBuyRepository.increaseCurrentQuantityIfAvailable(groupBuyId, request.quantity)).thenReturn(0)

        val ex = assertThrows<CustomException> {
            service.createParticipation(userId, groupBuyId, request)
        }

        assertEquals(ErrorCode.GROUPBUY_SOLD_OUT, ex.errorCode)
        verify(redisLockUtil).unlock(key, token)
    }

    @Test
    fun `참여 성공 시 응답 반환 및 목표 달성 상태 전이`() {
        val userId = 1L
        val groupBuyId = 14L
        val request = ParticipationCreateRequest(quantity = 2)
        val key = "groupBuy:$groupBuyId"
        val token = "token-4"
        val user = createUser(userId)
        val beforeUpdate = createGroupBuy(
            id = groupBuyId,
            status = GroupBuyStatus.IN_PROGRESS,
            currentQuantity = 3,
            targetQuantity = 5,
            price = 6000
        )
        val afterUpdate = createGroupBuy(
            id = groupBuyId,
            status = GroupBuyStatus.IN_PROGRESS,
            currentQuantity = 5,
            targetQuantity = 5,
            price = 6000
        )

        stubBase(userId, groupBuyId, key, token, user, beforeUpdate)
        `when`(groupBuyRepository.findById(groupBuyId)).thenReturn(Optional.of(beforeUpdate), Optional.of(afterUpdate))
        `when`(groupBuyRepository.increaseCurrentQuantityIfAvailable(groupBuyId, request.quantity)).thenReturn(1)
        `when`(participationRepository.save(any(Participation::class.java))).thenAnswer { invocation ->
            val saved = invocation.getArgument<Participation>(0)
            saved.id = 999L
            saved
        }

        val result = service.createParticipation(userId, groupBuyId, request)

        assertEquals(999L, result.participationId)
        assertEquals("두쫀쿠 2개", result.orderName)
        assertEquals(12_000, result.productAmount)
        assertEquals(12_000, result.totalAmount)
        assertEquals(0, result.feeAmount)
        assertTrue(afterUpdate.status == GroupBuyStatus.ACHIEVED)
        verify(redisLockUtil).unlock(key, token)
    }

    private fun stubBase(
        userId: Long,
        groupBuyId: Long,
        key: String,
        token: String,
        user: User,
        groupBuy: GroupBuy
    ) {
        `when`(participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)).thenReturn(false)
        `when`(redisLockUtil.lockKey(groupBuyId)).thenReturn(key)
        `when`(redisLockUtil.tryLockOrThrow(key, 500, 3_000)).thenReturn(token)
        `when`(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(user)
        `when`(groupBuyRepository.findById(groupBuyId)).thenReturn(Optional.of(groupBuy))
        `when`(redisLockUtil.unlock(key, token)).thenReturn(true)
    }
}
