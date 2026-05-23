package com.moongchijang.domain.participation.application

import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ParticipationPickupCommandService(
    private val participationRepository: ParticipationRepository,
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun completePickup(participationId: Long, processedByUserId: Long, pickedUpAt: LocalDateTime = LocalDateTime.now()) {
        val participation = participationRepository.findByIdForUpdate(participationId)
            .orElseThrow { CustomException(ErrorCode.PARTICIPATION_NOT_FOUND) }
        val processor = userRepository.findByIdAndDeletedAtIsNull(processedByUserId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        participation.markPickedUp(processedBy = processor, pickedUpAt = pickedUpAt)
        log.info(
            "[ParticipationPickupCommandService] 픽업 완료 처리: participationId={}, processedByUserId={}",
            participationId, processedByUserId
        )
    }
}
