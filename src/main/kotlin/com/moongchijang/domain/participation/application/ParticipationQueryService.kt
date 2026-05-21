package com.moongchijang.domain.participation.application

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyProgressCalculator
import com.moongchijang.domain.participation.application.dto.InProgressParticipationItemResponse
import com.moongchijang.domain.participation.application.dto.InProgressParticipationPageResponse
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class ParticipationQueryService(
    private val participationRepository: ParticipationRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getInProgressParticipations(userId: Long, pageable: Pageable): InProgressParticipationPageResponse {
        log.info(
            "[ParticipationQueryService] 진행 중 참여 내역 조회 시작: userId={}, page={}, size={}",
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
            "[ParticipationQueryService] 진행 중 참여 내역 조회 완료: userId={}, contentSize={}, totalElements={}, totalPages={}",
            userId,
            page.content.size,
            page.totalElements,
            page.totalPages
        )

        val mapped = page.map { participation -> toInProgressItem(participation) }
        return InProgressParticipationPageResponse.from(mapped)
    }

    private fun toInProgressItem(participation: Participation): InProgressParticipationItemResponse {
        val groupBuy = participation.groupBuy
        val dDay = ChronoUnit.DAYS.between(
            LocalDateTime.now().toLocalDate(),
            groupBuy.deadline.toLocalDate()
        ).toInt()

        return InProgressParticipationItemResponse(
            participationId = participation.id,
            groupbuyId = groupBuy.id,
            productName = groupBuy.productName,
            storeName = groupBuy.store.name,
            pickupAt = LocalDateTime.of(groupBuy.pickupDate, groupBuy.pickupTimeStart),
            paidAmount = participation.totalAmount,
            quantity = participation.quantity,
            achievementRate = GroupBuyProgressCalculator.achievementRate(
                currentQuantity = groupBuy.currentQuantity,
                targetQuantity = groupBuy.targetQuantity
            ),
            dDay = dDay,
            participatedAt = participation.createdAt!!
        )
    }

    companion object {
        private val IN_PROGRESS_STATUSES = listOf(
            ParticipationStatus.PAID_WAITING_GOAL,
            ParticipationStatus.CONFIRMED
        )
    }
}
