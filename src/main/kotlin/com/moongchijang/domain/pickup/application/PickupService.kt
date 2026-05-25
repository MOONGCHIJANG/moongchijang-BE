package com.moongchijang.domain.pickup.application

import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.pickup.application.dto.NearestPickupQrReason
import com.moongchijang.domain.pickup.application.dto.NearestPickupQrResponse
import com.moongchijang.domain.pickup.application.dto.PickupAvailabilityStatus
import com.moongchijang.domain.pickup.application.dto.PickupGuideResponse
import com.moongchijang.domain.pickup.application.dto.PickupQrResponse
import com.moongchijang.domain.pickup.application.dto.PickupVerifyResponse
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class PickupService(
    private val participationRepository: ParticipationRepository,
    private val userRepository: UserRepository,
) {

    @Transactional(readOnly = true)
    fun getPickupGuide(participationId: Long, userId: Long): PickupGuideResponse {
        val participation = findOwnedParticipation(participationId, userId)
        val groupBuy = participation.groupBuy
        val store = groupBuy.store

        return PickupGuideResponse(
            participationId = participation.id,
            availabilityStatus = participation.availabilityStatus(),
            pickupStatus = participation.pickupStatus,
            storeName = store.name,
            storeAddress = store.address,
            storePhone = store.phoneNumber,
            latitude = store.latitude,
            longitude = store.longitude,
            transitInfo = null,
            thumbnailUrl = groupBuy.thumbnailUrl,
            productName = groupBuy.productName,
            quantity = participation.quantity,
            pickupDate = groupBuy.pickupDate,
            pickupTimeStart = groupBuy.pickupTimeStart,
            pickupTimeEnd = groupBuy.pickupTimeEnd,
            pickupLocation = groupBuy.pickupLocation,
            remainingMinutes = participation.remainingPickupMinutes(),
            pickedUpAt = participation.pickedUpAt,
        )
    }

    @Transactional
    fun getPickupQr(participationId: Long, userId: Long): PickupQrResponse {
        val participation = findOwnedParticipation(participationId, userId)
        return buildPickupQrResponse(participation)
    }

    @Transactional
    fun getNearestPickupQr(userId: Long): NearestPickupQrResponse {
        val today = todayKst()
        val candidates = participationRepository.findNearestPickupQrCandidates(
            userId = userId,
            status = ParticipationStatus.CONFIRMED,
            pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
            fromDate = today,
        )

        if (candidates.isEmpty()) {
            return NearestPickupQrResponse(
                hasCandidate = false,
                hasMultipleToday = false,
                reason = NearestPickupQrReason.NO_AVAILABLE_PICKUP,
                item = null,
            )
        }

        val todayCandidates = candidates.filter { it.groupBuy.pickupDate == today }
        val selected = todayCandidates.firstOrNull() ?: candidates.first()

        return NearestPickupQrResponse(
            hasCandidate = true,
            hasMultipleToday = todayCandidates.size > 1,
            reason = if (todayCandidates.isEmpty()) NearestPickupQrReason.ONLY_FUTURE_PICKUP else null,
            item = buildPickupQrResponse(selected),
        )
    }

    private fun buildPickupQrResponse(participation: Participation): PickupQrResponse {
        val isActive = participation.isPickupActive()
        if (isActive && participation.pickupStatus == PickupStatus.NOT_READY) {
            participation.pickupStatus = PickupStatus.READY
        }
        if (isActive && participation.pickupStatus == PickupStatus.READY && participation.pickupToken == null) {
            participation.pickupToken = generateUniquePickupToken()
        }

        return PickupQrResponse(
            participationId = participation.id,
            reservationNumber = participation.reservationNumber(),
            availabilityStatus = participation.availabilityStatus(),
            pickupStatus = participation.pickupStatus,
            userName = participation.user.nickname,
            productName = participation.groupBuy.productName,
            quantity = participation.quantity,
            storeName = participation.groupBuy.store.name,
            storeAddress = participation.groupBuy.store.address,
            pickupLocation = participation.groupBuy.pickupLocation,
            qrCode = participation.pickupToken.takeIf { isActive && participation.pickupStatus == PickupStatus.READY },
            pickupDate = participation.groupBuy.pickupDate,
            pickupTimeStart = participation.groupBuy.pickupTimeStart,
            pickupTimeEnd = participation.groupBuy.pickupTimeEnd,
            dDay = participation.pickupDDay(),
            pickedUpAt = participation.pickedUpAt,
        )
    }

    @Transactional
    fun verifyPickup(qrCode: String, processedByUserId: Long): PickupVerifyResponse {
        val participation = participationRepository.findByPickupTokenForUpdate(qrCode)
            ?: throw CustomException(ErrorCode.PICKUP_QR_NOT_FOUND)

        if (!participation.isPickupActive()) {
            throw CustomException(ErrorCode.PICKUP_LOCKED)
        }
        if (participation.pickupStatus == PickupStatus.PICKED_UP) {
            throw CustomException(ErrorCode.PICKUP_ALREADY_USED)
        }

        val processedBy = userRepository.findByIdAndDeletedAtIsNull(processedByUserId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val pickedUpAt = LocalDateTime.now()
        participation.markPickedUp(processedBy, pickedUpAt)

        return PickupVerifyResponse(
            participationId = participation.id,
            pickupStatus = participation.pickupStatus,
            pickedUpAt = pickedUpAt,
            pickupProcessedByUserId = processedBy?.id,
        )
    }

    private fun findOwnedParticipation(participationId: Long, userId: Long): Participation {
        val participation = participationRepository.findPickupDetailById(participationId)
            ?: throw CustomException(ErrorCode.PARTICIPATION_NOT_FOUND)

        if (participation.user.id != userId) {
            throw CustomException(ErrorCode.PICKUP_PARTICIPATION_FORBIDDEN)
        }
        return participation
    }

    private fun Participation.isPickupActive(): Boolean =
        !todayKst().isBefore(groupBuy.pickupDate)

    private fun Participation.availabilityStatus(): PickupAvailabilityStatus =
        when {
            pickupStatus == PickupStatus.PICKED_UP -> PickupAvailabilityStatus.PICKED_UP
            isPickupActive() -> PickupAvailabilityStatus.AVAILABLE
            else -> PickupAvailabilityStatus.LOCKED
        }

    private fun Participation.remainingPickupMinutes(): Long? {
        val today = todayKst()
        if (groupBuy.pickupDate != today) return null

        val now = nowKst()
        val pickupEndAt = LocalDateTime.of(groupBuy.pickupDate, groupBuy.pickupTimeEnd)
        return ChronoUnit.MINUTES.between(now, pickupEndAt).coerceAtLeast(0)
    }

    private fun Participation.pickupDDay(): Int =
        ChronoUnit.DAYS.between(todayKst(), groupBuy.pickupDate)
            .toInt()

    private fun Participation.reservationNumber(): String =
        "MCJ-P%06d".format(id)

    private fun todayKst(): LocalDate = LocalDate.now(KST_ZONE)

    private fun nowKst(): LocalDateTime = LocalDateTime.now(KST_ZONE)

    private fun generateUniquePickupToken(): String {
        repeat(TOKEN_GENERATION_ATTEMPTS) {
            val token = UUID.randomUUID().toString()
            if (!participationRepository.existsByPickupToken(token)) {
                return token
            }
        }
        throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
    }

    companion object {
        private const val TOKEN_GENERATION_ATTEMPTS = 5
        private val KST_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
