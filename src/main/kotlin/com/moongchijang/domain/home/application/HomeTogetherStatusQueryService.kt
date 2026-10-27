package com.moongchijang.domain.home.application

import com.moongchijang.domain.home.application.dto.HomeTodayPickupResponse
import com.moongchijang.domain.home.application.dto.HomeTogetherRoomAction
import com.moongchijang.domain.home.application.dto.HomeTogetherRoomSummaryResponse
import com.moongchijang.domain.home.application.dto.HomeTogetherStatusResponse
import com.moongchijang.domain.together.domain.entity.TogetherRoom
import com.moongchijang.domain.together.domain.entity.TogetherRoomMemberRole
import com.moongchijang.domain.together.domain.entity.TogetherRoomStatus
import com.moongchijang.domain.together.domain.repository.ParticipantSettlementRepository
import com.moongchijang.domain.together.domain.repository.PickupQrCodeRepository
import com.moongchijang.domain.together.domain.repository.TogetherRoomMemberRepository
import com.moongchijang.domain.together.domain.repository.TogetherRoomRepository
import com.moongchijang.global.time.kstNow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class HomeTogetherStatusQueryService(
    private val togetherRoomRepository: TogetherRoomRepository,
    private val togetherRoomMemberRepository: TogetherRoomMemberRepository,
    private val participantSettlementRepository: ParticipantSettlementRepository,
    private val pickupQrCodeRepository: PickupQrCodeRepository,
    private val clock: Clock,
) {

    fun getStatus(userId: Long?): HomeTogetherStatusResponse {
        if (userId == null) {
            return HomeTogetherStatusResponse(
                hasActiveRooms = false,
                showTogetherRoomModule = false,
                togetherTabBadgeCount = 0,
                isTodayPickupLeader = false,
                todayPickups = emptyList(),
                rooms = emptyList(),
            )
        }

        val now = clock.kstNow()
        val memberRooms = togetherRoomMemberRepository.findAllActiveWithRoomByUserId(userId)
        val roomRoles = memberRooms.associate { member -> member.room.id to member.role }.toMutableMap()
        val activeRooms = linkedMapOf<Long, TogetherRoom>()
        memberRooms
            .map { it.room }
            .filterNot { it.status in TERMINAL_STATUSES }
            .forEach { room -> activeRooms[room.id] = room }
        togetherRoomRepository.findActiveByLeaderId(userId, TERMINAL_STATUSES).forEach { room ->
            activeRooms[room.id] = room
            roomRoles[room.id] = TogetherRoomMemberRole.LEADER
        }

        if (activeRooms.isEmpty()) {
            return HomeTogetherStatusResponse(
                hasActiveRooms = false,
                showTogetherRoomModule = false,
                togetherTabBadgeCount = 0,
                isTodayPickupLeader = false,
                todayPickups = emptyList(),
                rooms = emptyList(),
            )
        }

        val rooms = activeRooms.values.toList()
        val roomIds = rooms.map { it.id }
        val pendingTransferRoomIds = participantSettlementRepository
            .findRoomIdsByParticipantIdAndStatus(userId)
            .intersect(roomIds.toSet())
        val todayPickupRooms = rooms.filter { room ->
            room.status == TogetherRoomStatus.PICKUP_READY &&
                room.pickupDate == now.toLocalDate() &&
                room.pickupTimeEnd > now.toLocalTime()
        }
        val leaderTodayPickupRoomIds = todayPickupRooms
            .filter { roomRoles[it.id] == TogetherRoomMemberRole.LEADER }
            .map { it.id }
            .toSet()
        val qrAvailableRoomIds = if (leaderTodayPickupRoomIds.isEmpty()) {
            emptySet()
        } else {
            pickupQrCodeRepository.findActiveRoomIds(leaderTodayPickupRoomIds, now)
        }

        val actionsByRoomId = rooms.associate { room ->
            room.id to determineAction(
                room = room,
                role = roomRoles.getValue(room.id),
                pendingTransferRoomIds = pendingTransferRoomIds,
                leaderTodayPickupRoomIds = leaderTodayPickupRoomIds,
            )
        }
        val todayPickupRoomIdSet = todayPickupRooms.map { it.id }.toSet()
        val summaries = rooms
            .asSequence()
            .filterNot { it.id in todayPickupRoomIdSet }
            .sortedWith(
                compareBy<TogetherRoom> { room -> actionOrder(actionsByRoomId.getValue(room.id)) }
                    .thenBy { room -> recruitmentOrder(room.status) }
                    .thenBy { it.paymentDeadlineAt }
                    .thenBy { it.id },
            )
            .map { room ->
                HomeTogetherRoomSummaryResponse(
                    roomId = room.id,
                    role = roomRoles.getValue(room.id),
                    productId = room.product.id,
                    productName = room.product.name,
                    storeName = room.store.name,
                    currentQuantity = room.currentQuantity,
                    minQuantity = room.product.minQuantity,
                    maxQuantity = room.product.maxQuantity,
                    paymentDeadlineAt = room.paymentDeadlineAt,
                    nextAction = actionsByRoomId.getValue(room.id),
                )
            }
            .toList()

        return HomeTogetherStatusResponse(
            hasActiveRooms = true,
            showTogetherRoomModule = summaries.isNotEmpty(),
            togetherTabBadgeCount = actionsByRoomId.values.count { it != HomeTogetherRoomAction.NONE },
            isTodayPickupLeader = leaderTodayPickupRoomIds.isNotEmpty(),
            todayPickups = todayPickupRooms
                .sortedBy { it.pickupTimeStart }
                .map { room ->
                    HomeTodayPickupResponse(
                        roomId = room.id,
                        role = roomRoles.getValue(room.id),
                        productName = room.product.name,
                        quantity = room.currentQuantity,
                        storeName = room.store.name,
                        pickupDate = room.pickupDate,
                        pickupTimeStart = room.pickupTimeStart,
                        pickupTimeEnd = room.pickupTimeEnd,
                        canOpenQr = room.id in qrAvailableRoomIds && room.id in leaderTodayPickupRoomIds,
                    )
                },
            rooms = summaries,
        )
    }

    private fun determineAction(
        room: TogetherRoom,
        role: TogetherRoomMemberRole,
        pendingTransferRoomIds: Set<Long>,
        leaderTodayPickupRoomIds: Set<Long>,
    ): HomeTogetherRoomAction = when {
        room.id in leaderTodayPickupRoomIds -> HomeTogetherRoomAction.PICKUP
        role == TogetherRoomMemberRole.PARTICIPANT && room.id in pendingTransferRoomIds -> HomeTogetherRoomAction.TRANSFER
        role == TogetherRoomMemberRole.LEADER &&
            room.status in PAYMENT_AVAILABLE_STATUSES &&
            room.currentQuantity >= room.product.minQuantity -> HomeTogetherRoomAction.PAY
        else -> HomeTogetherRoomAction.NONE
    }

    private fun actionOrder(action: HomeTogetherRoomAction): Int =
        if (action == HomeTogetherRoomAction.NONE) 1 else 0

    private fun recruitmentOrder(status: TogetherRoomStatus): Int =
        if (status == TogetherRoomStatus.RECRUITING) 0 else 1

    companion object {
        private val TERMINAL_STATUSES = setOf(
            TogetherRoomStatus.PICKUP_COMPLETED,
            TogetherRoomStatus.CLOSED,
            TogetherRoomStatus.CANCELLED,
        )
        private val PAYMENT_AVAILABLE_STATUSES = setOf(
            TogetherRoomStatus.RECRUITING,
            TogetherRoomStatus.PAYMENT_READY,
        )
    }
}
