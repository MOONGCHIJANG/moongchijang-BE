package com.moongchijang.domain.participation.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.infrastructure.lock.RedisLockUtil
import com.moongchijang.domain.participation.application.dto.ParticipationCreateRequest
import com.moongchijang.domain.participation.application.dto.ParticipationCreatedResponse
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class ParticipationService(
    private val redisLockUtil: RedisLockUtil,
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val userRepository: UserRepository,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun createParticipation(
        userId: Long,
        groupBuyId: Long,
        request: ParticipationCreateRequest
    ): ParticipationCreatedResponse {
        val (key, token) = acquireLock(groupBuyId)

        try {
            val response = transactionTemplate.execute {
                validateNotParticipated(userId, groupBuyId)

                val user = findUser(userId)
                val groupBuy = findGroupBuy(groupBuyId)

                validateCanParticipate(groupBuy, request.quantity)
                increaseQuantityOrThrow(groupBuyId, request.quantity)

                val updatedGroupBuy = findGroupBuy(groupBuyId)
                updateStatusIfAchieved(updatedGroupBuy)

                val participation = createParticipation(user, groupBuy, request.quantity)

                // TODO(MCJ-1448): 결제 시스템 연동
                // 결제 요청 생성/승인/검증 로직 연결
                // 결제 진입 직전 최종 재검증 연결

                log.info(
                    "[ParticipationService] 참여 생성 완료: participationId={}, groupBuyId={}, quantity={}",
                    participation.id, groupBuyId, request.quantity
                )
                toCreatedResponse(participation, groupBuy)
            }

            return response
        } finally {
            releaseLock(key, token)
        }
    }

    private fun validateNotParticipated(userId: Long, groupBuyId: Long) {
        if (participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)) {
            throw CustomException(ErrorCode.GROUPBUY_ALREADY_PARTICIPATED)
        }
    }

    private fun acquireLock(groupBuyId: Long): Pair<String, String> {
        val key = redisLockUtil.lockKey(groupBuyId)
        val token = redisLockUtil.tryLockOrThrow(key, waitMs = 500, leaseMs = 3_000)
        return key to token
    }

    private fun releaseLock(key: String, token: String) {
        val unlocked = redisLockUtil.unlock(key, token)
        if (!unlocked) {
            log.warn("[ParticipationService] 락 해제 실패: key={}", key)
        } else {
            log.debug("[ParticipationService] 락 해제 성공: key={}", key)
        }
    }

    private fun findUser(userId: Long) =
        userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

    private fun findGroupBuy(groupBuyId: Long) =
        groupBuyRepository.findById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

    private fun increaseQuantityOrThrow(groupBuyId: Long, quantity: Int) {
        val updatedRows = groupBuyRepository.increaseCurrentQuantityIfAvailable(groupBuyId, quantity)
        if (updatedRows == 0) {
            log.warn(
                "[ParticipationService] 참여 불가 - 조건부 차감 실패: groupBuyId={}, requestQuantity={}",
                groupBuyId, quantity
            )
            throw CustomException(ErrorCode.GROUPBUY_SOLD_OUT)
        }
    }

    private fun createParticipation(user: User, groupBuy: GroupBuy, quantity: Int): Participation {
        val productAmount = groupBuy.price * quantity
        val feeAmount = 0
        val totalAmount = productAmount + feeAmount

        return participationRepository.save(
            Participation(
                user = user,
                groupBuy = groupBuy,
                quantity = quantity,
                productAmount = productAmount,
                feeAmount = feeAmount,
                totalAmount = totalAmount,
                status = ParticipationStatus.PENDING,
                pickupStatus = PickupStatus.NOT_READY,
            )
        )
    }

    private fun toCreatedResponse(participation: Participation, groupBuy: GroupBuy): ParticipationCreatedResponse {
        return ParticipationCreatedResponse(
            participationId = participation.id,
            orderName = "${groupBuy.productName} ${participation.quantity}개",
            totalAmount = participation.totalAmount,
            productAmount = participation.productAmount,
            feeAmount = participation.feeAmount
        )
    }

    private fun validateCanParticipate(groupBuy: GroupBuy, quantity: Int) {
        if (groupBuy.status != GroupBuyStatus.IN_PROGRESS) {
            log.warn(
                "[ParticipationService] 참여 불가 - 모집 상태 아님: groupBuyId={}, status={}",
                groupBuy.id, groupBuy.status
            )
            throw CustomException(ErrorCode.GROUPBUY_NOT_RECRUITING)
        }

        if (quantity <= 0) {
            throw CustomException(ErrorCode.INVALID_INPUT)
        }
    }

    private fun updateStatusIfAchieved(groupBuy: GroupBuy) {
        if (groupBuy.status == GroupBuyStatus.IN_PROGRESS &&
            groupBuy.currentQuantity >= groupBuy.targetQuantity) {
            groupBuy.status = GroupBuyStatus.ACHIEVED
            log.info(
                "[ParticipationService] 상태 전이: groupBuyId={}, status={}",
                groupBuy.id, groupBuy.status
            )
        }
    }
}
