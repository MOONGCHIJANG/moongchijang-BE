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
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ParticipationService(
    private val redisLockUtil: RedisLockUtil,
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createParticipation(
        userId: Long,
        groupBuyId: Long,
        request: ParticipationCreateRequest
    ): ParticipationCreatedResponse {
        val key = redisLockUtil.lockKey(groupBuyId)
        val token = redisLockUtil.tryLockOrThrow(key, waitMs = 500, leaseMs = 3_000)

        try {
            val user = userRepository.findByIdAndDeletedAtIsNull(userId)
                ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

            val groupBuy = groupBuyRepository.findById(groupBuyId)
                .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

            validateCanParticipate(groupBuy, request.quantity)

            // TODO(MCJ-1448): 조건부 차감 쿼리로 교체
            //  - where id = :groupBuyId and (max_quantity - current_quantity) >= :quantity
            //  - update current_quantity = current_quantity + :quantity
            val remaining = groupBuy.maxQuantity - groupBuy.currentQuantity
            if (remaining < request.quantity) {
                log.warn(
                    "[ParticipationService] 참여 불가 - 수량 부족: groupBuyId={}, remaining={}, requestQty={}",
                    groupBuyId, remaining, request.quantity
                )
                throw CustomException(ErrorCode.GROUPBUY_SOLD_OUT)
            }

            groupBuy.currentQuantity += request.quantity
            updateStatusIfAchieved(groupBuy)

            val productAmount = groupBuy.price * request.quantity
            val feeAmount = 0
            val totalAmount = productAmount + feeAmount

            val participation = participationRepository.save(
                Participation(
                    user = user,
                    groupBuy = groupBuy,
                    quantity = request.quantity,
                    productAmount = productAmount,
                    feeAmount = feeAmount,
                    totalAmount = totalAmount,
                    status = ParticipationStatus.PENDING,
                    pickupStatus = PickupStatus.NOT_READY,
                )
            )

            // TODO(MCJ-1448): 결제 시스템 연동
            // 결제 요청 생성/승인/검증 로직 연결
            // 결제 진입 직전 최종 재검증 연결

            log.info(
                "[ParticipationService] 참여 생성 완료: participationId={}, groupBuyId={}, quantity={}",
                participation.id, groupBuyId, request.quantity
            )

            return ParticipationCreatedResponse(
                participationId = participation.id,
                orderName = "${groupBuy.productName} ${request.quantity}개",
                totalAmount = totalAmount,
                productAmount = productAmount,
                feeAmount = feeAmount
            )
        } finally {
            val unlocked = redisLockUtil.unlock(key, token)
            if (!unlocked) {
                log.warn("[ParticipationService] 락 해제 실패: key={}", key)
            } else {
                log.debug("[ParticipationService] 락 해제 성공: key={}", key)
            }
        }
    }

    private fun validateCanParticipate(groupBuy: GroupBuy, quantity: Int) {
        if (groupBuy.status != GroupBuyStatus.IN_PROGRESS) {
            log.warn(
                "[ParticipationService] 참여 불가 - 모집 상태 아님: groupBuyId={}, status={}",
                groupBuy.id, groupBuy.status
            )
            throw CustomException(ErrorCode.GROUPBUY_NOT_RECRUITING)
        }

        if (!LocalDateTime.now().isBefore(groupBuy.deadline)) {
            log.warn(
                "[ParticipationService] 참여 불가 - 마감 지남: groupBuyId={}, deadline={}",
                groupBuy.id, groupBuy.deadline
            )
            throw CustomException(ErrorCode.GROUPBUY_DEADLINE_PASSED)
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
