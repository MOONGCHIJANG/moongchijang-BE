package com.moongchijang.domain.mypage.application

import com.moongchijang.domain.participation.application.dto.InProgressParticipationItemResponse
import com.moongchijang.domain.participation.application.dto.InProgressParticipationPageResponse
import com.moongchijang.domain.participation.application.dto.PickupWaitingParticipationItemResponse
import com.moongchijang.domain.participation.application.dto.PickupWaitingParticipationPageResponse
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.global.time.kstNow
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class MyPageParticipationQueryService(
    private val participationRepository: ParticipationRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getInProgressParticipations(userId: Long, pageable: Pageable): InProgressParticipationPageResponse {
        log.info(
            "[MyPageParticipationQueryService] 진행 중 참여 내역 조회 시작: userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )

        val page = participationRepository.findInProgressByUserId(
            userId = userId,
            statuses = IN_PROGRESS_STATUSES,
            pageable = pageable
        )

        log.info(
            "[MyPageParticipationQueryService] 진행 중 참여 내역 조회 완료: userId={}, contentSize={}, totalElements={}, totalPages={}",
            userId,
            page.content.size,
            page.totalElements,
            page.totalPages
        )

        val now = clock.kstNow()
        val mapped = page.map { participation -> InProgressParticipationItemResponse.from(participation, now) }
        return InProgressParticipationPageResponse.from(mapped)
    }

    @Transactional(readOnly = true)
    fun getPickupWaitingParticipations(userId: Long, pageable: Pageable): PickupWaitingParticipationPageResponse {
        log.info(
            "[MyPageParticipationQueryService] 픽업 대기 참여 내역 조회 시작: userId={}, page={}, size={}",
            userId,
            pageable.pageNumber,
            pageable.pageSize
        )

        val page = participationRepository.findPickupWaitingByUserId(
            userId = userId,
            participationStatuses = PICKUP_WAITING_PARTICIPATION_STATUSES,
            pickupStatuses = PICKUP_WAITING_PICKUP_STATUSES,
            pageable = pageable
        )

        log.info(
            "[MyPageParticipationQueryService] 픽업 대기 참여 내역 조회 완료: userId={}, contentSize={}, totalElements={}, totalPages={}",
            userId,
            page.content.size,
            page.totalElements,
            page.totalPages
        )

        val mapped = page.map { participation -> PickupWaitingParticipationItemResponse.from(participation) }
        return PickupWaitingParticipationPageResponse.from(mapped)
    }

    companion object {
        private val IN_PROGRESS_STATUSES = listOf(
            ParticipationStatus.PAID_WAITING_GOAL
        )
        private val PICKUP_WAITING_PARTICIPATION_STATUSES = listOf(
            ParticipationStatus.CONFIRMED
        )
        private val PICKUP_WAITING_PICKUP_STATUSES = listOf(
            PickupStatus.NOT_READY,
            PickupStatus.READY
        )
    }
}
