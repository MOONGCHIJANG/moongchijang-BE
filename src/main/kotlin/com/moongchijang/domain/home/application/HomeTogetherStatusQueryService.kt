package com.moongchijang.domain.home.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.RoomPickupRepository
import com.moongchijang.domain.groupbuy.domain.repository.SettlementTransferRepository
import com.moongchijang.domain.home.application.dto.HomeTodayPickupResponse
import com.moongchijang.domain.home.application.dto.HomeTogetherRole
import com.moongchijang.domain.home.application.dto.HomeTogetherRoomAction
import com.moongchijang.domain.home.application.dto.HomeTogetherRoomSummaryResponse
import com.moongchijang.domain.home.application.dto.HomeTogetherStatusResponse
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.global.time.kstNow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class HomeTogetherStatusQueryService(
    private val groupBuyRepository: GroupBuyRepository,
    private val participationRepository: ParticipationRepository,
    private val settlementTransferRepository: SettlementTransferRepository,
    private val roomPickupRepository: RoomPickupRepository,
    private val clock: Clock,
) {

    fun getStatus(userId: Long?): HomeTogetherStatusResponse {
        if (userId == null) return emptyResponse(showFirstVisitGuide = true)

        val now = clock.kstNow()
        val activeRooms = linkedMapOf<Long, GroupBuy>()
        val roles = mutableMapOf<Long, HomeTogetherRole>()
        participationRepository.findAllActiveWithGroupBuyByUserId(userId)
            .map { it.groupBuy }
            .filter { it.status in ACTIVE_STATUSES }
            .forEach { room ->
                activeRooms[room.id] = room
                roles[room.id] = HomeTogetherRole.PARTICIPANT
            }
        groupBuyRepository.findActiveByOrganizerId(userId, ACTIVE_STATUSES).forEach { room ->
            activeRooms[room.id] = room
            roles[room.id] = HomeTogetherRole.ORGANIZER
        }
        if (activeRooms.isEmpty()) {
            val hasTogetherExperience = groupBuyRepository.existsByOrganizerId(userId) ||
                participationRepository.existsByUserId(userId)
            return emptyResponse(showFirstVisitGuide = !hasTogetherExperience)
        }

        val rooms = activeRooms.values.toList()
        val roomIds = rooms.map { it.id }
        val pendingTransferRoomIds = settlementTransferRepository
            .findGroupBuyIdsByParticipantIdAndStatus(userId)
            .intersect(roomIds.toSet())
        val todayPickupRooms = rooms.filter { room ->
            room.paidAt != null &&
                room.pickupDate == now.toLocalDate() &&
                room.pickupTimeEnd > now.toLocalTime()
        }
        val organizerTodayPickupIds = todayPickupRooms
            .filter { roles[it.id] == HomeTogetherRole.ORGANIZER }
            .map { it.id }
            .toSet()
        val qrAvailableRoomIds = if (organizerTodayPickupIds.isEmpty()) emptySet() else {
            roomPickupRepository.findActiveGroupBuyIds(organizerTodayPickupIds, now)
        }
        val actionsByRoomId = rooms.associate { room ->
            room.id to determineAction(
                room = room,
                role = roles.getValue(room.id),
                pendingTransferRoomIds = pendingTransferRoomIds,
                organizerTodayPickupIds = organizerTodayPickupIds,
            )
        }
        val todayPickupIds = todayPickupRooms.map { it.id }.toSet()
        val summaries = rooms.asSequence()
            .filterNot { it.id in todayPickupIds }
            .sortedWith(
                compareBy<GroupBuy> { actionOrder(actionsByRoomId.getValue(it.id)) }
                    .thenBy { recruitmentOrder(it.status) }
                    .thenBy { it.paymentDeadline ?: it.deadline }
                    .thenBy { it.id },
            )
            .map { room -> room.toSummary(roles.getValue(room.id), actionsByRoomId.getValue(room.id)) }
            .toList()

        return HomeTogetherStatusResponse(
            hasActiveRooms = true,
            showTogetherRoomModule = summaries.isNotEmpty(),
            showFirstVisitGuide = false,
            togetherTabBadgeCount = actionsByRoomId.values.count { it != HomeTogetherRoomAction.NONE },
            isTodayPickupLeader = organizerTodayPickupIds.isNotEmpty(),
            todayPickups = todayPickupRooms.sortedBy { it.pickupTimeStart }.map { room ->
                HomeTodayPickupResponse(
                    roomId = room.id,
                    role = roles.getValue(room.id),
                    productName = room.productName,
                    quantity = room.currentQuantity,
                    storeName = room.store.name,
                    pickupDate = room.pickupDate,
                    pickupTimeStart = room.pickupTimeStart,
                    pickupTimeEnd = room.pickupTimeEnd,
                    canOpenQr = room.id in organizerTodayPickupIds && room.id in qrAvailableRoomIds,
                )
            },
            rooms = summaries,
        )
    }

    private fun GroupBuy.toSummary(
        role: HomeTogetherRole,
        nextAction: HomeTogetherRoomAction,
    ) = HomeTogetherRoomSummaryResponse(
        roomId = id,
        role = role,
        productId = product?.id,
        productName = productName,
        storeName = store.name,
        currentQuantity = currentQuantity,
        minQuantity = minQuantity ?: targetQuantity,
        maxQuantity = maxQuantity,
        paymentDeadlineAt = paymentDeadline ?: deadline,
        nextAction = nextAction,
    )

    private fun determineAction(
        room: GroupBuy,
        role: HomeTogetherRole,
        pendingTransferRoomIds: Set<Long>,
        organizerTodayPickupIds: Set<Long>,
    ): HomeTogetherRoomAction = when {
        room.id in organizerTodayPickupIds -> HomeTogetherRoomAction.PICKUP
        role == HomeTogetherRole.PARTICIPANT && room.id in pendingTransferRoomIds -> HomeTogetherRoomAction.TRANSFER
        role == HomeTogetherRole.ORGANIZER &&
            room.paidAt == null &&
            room.currentQuantity >= (room.minQuantity ?: room.targetQuantity) -> HomeTogetherRoomAction.PAY
        else -> HomeTogetherRoomAction.NONE
    }

    private fun actionOrder(action: HomeTogetherRoomAction): Int =
        if (action == HomeTogetherRoomAction.NONE) 1 else 0

    private fun recruitmentOrder(status: GroupBuyStatus): Int =
        if (status == GroupBuyStatus.IN_PROGRESS) 0 else 1

    private fun emptyResponse(showFirstVisitGuide: Boolean = false) = HomeTogetherStatusResponse(
        hasActiveRooms = false,
        showTogetherRoomModule = false,
        showFirstVisitGuide = showFirstVisitGuide,
        togetherTabBadgeCount = 0,
        isTodayPickupLeader = false,
        todayPickups = emptyList(),
        rooms = emptyList(),
    )

    companion object {
        private val ACTIVE_STATUSES = setOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED)
    }
}
