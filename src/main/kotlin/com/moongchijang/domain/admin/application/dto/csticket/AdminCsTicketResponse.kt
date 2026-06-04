package com.moongchijang.domain.admin.application.dto.csticket

import com.moongchijang.domain.csticket.domain.entity.CsTicket
import com.moongchijang.domain.csticket.domain.entity.CsTicketPriority
import com.moongchijang.domain.csticket.domain.entity.CsTicketStatus
import com.moongchijang.domain.csticket.domain.entity.CsTicketType
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Page
import java.time.Duration
import java.time.LocalDateTime

enum class AdminCsTicketStatusFilter {
    RECEIVED,
    IN_PROGRESS,
    COMPLETED,
    ALL;

    fun toStatus(): CsTicketStatus? =
        when (this) {
            RECEIVED -> CsTicketStatus.RECEIVED
            IN_PROGRESS -> CsTicketStatus.IN_PROGRESS
            COMPLETED -> CsTicketStatus.COMPLETED
            ALL -> null
        }
}

data class AdminCsTicketPageResponse(
    val content: List<AdminCsTicketListItemResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
) {
    companion object {
        fun from(page: Page<CsTicket>, now: LocalDateTime): AdminCsTicketPageResponse =
            AdminCsTicketPageResponse(
                content = page.content.map { AdminCsTicketListItemResponse.from(it, now) },
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                number = page.number,
                size = page.size
            )
    }
}

data class AdminCsTicketListItemResponse(
    val ticketId: Long,
    val type: CsTicketType,
    val title: String,
    val consumerId: Long?,
    val consumerName: String?,
    val groupBuyId: Long?,
    val groupBuyName: String?,
    val priority: CsTicketPriority,
    val assigneeName: String?,
    val createdAt: LocalDateTime?,
    val slaHours: Long,
    val status: CsTicketStatus,
    val actionable: Boolean,
) {
    companion object {
        fun from(ticket: CsTicket, now: LocalDateTime): AdminCsTicketListItemResponse =
            AdminCsTicketListItemResponse(
                ticketId = ticket.id,
                type = ticket.type,
                title = ticket.title,
                consumerId = ticket.consumer?.id,
                consumerName = ticket.consumer?.nickname,
                groupBuyId = ticket.groupBuy?.id,
                groupBuyName = ticket.groupBuy?.productName,
                priority = ticket.priority,
                assigneeName = ticket.assigneeName,
                createdAt = ticket.createdAt,
                slaHours = ticket.calculateSlaHours(now),
                status = ticket.status,
                actionable = ticket.status != CsTicketStatus.COMPLETED
            )
    }
}

data class AdminCsTicketDetailResponse(
    val ticketId: Long,
    val type: CsTicketType,
    val title: String,
    val description: String,
    val priority: CsTicketPriority,
    val status: CsTicketStatus,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val slaHours: Long,
    val consumer: AdminCsTicketUserResponse?,
    val owner: AdminCsTicketOwnerResponse?,
    val groupBuy: AdminCsTicketGroupBuyResponse?,
    val refundParticipationId: Long?,
    val assigneeName: String?,
    val processingMemo: String?,
    val resolvedAt: LocalDateTime?,
    val actionable: Boolean,
) {
    companion object {
        fun from(
            ticket: CsTicket,
            now: LocalDateTime,
            consumerEmail: String?,
            consumerPhoneNumber: String?,
        ): AdminCsTicketDetailResponse =
            AdminCsTicketDetailResponse(
                ticketId = ticket.id,
                type = ticket.type,
                title = ticket.title,
                description = ticket.description,
                priority = ticket.priority,
                status = ticket.status,
                createdAt = ticket.createdAt,
                updatedAt = ticket.updatedAt,
                slaHours = ticket.calculateSlaHours(now),
                consumer = ticket.consumer?.let { user ->
                    AdminCsTicketUserResponse(
                        userId = user.id,
                        nickname = user.nickname,
                        email = consumerEmail,
                        phoneNumber = consumerPhoneNumber
                    )
                },
                owner = ticket.groupBuy?.store?.let { store ->
                    AdminCsTicketOwnerResponse(
                        storeId = store.id,
                        storeName = store.name,
                        storePhoneNumber = store.phoneNumber
                    )
                },
                groupBuy = ticket.groupBuy?.let { groupBuy ->
                    AdminCsTicketGroupBuyResponse(
                        groupBuyId = groupBuy.id,
                        productName = groupBuy.productName,
                        storeName = groupBuy.store.name
                    )
                },
                refundParticipationId = ticket.refundParticipationId,
                assigneeName = ticket.assigneeName,
                processingMemo = ticket.processingMemo,
                resolvedAt = ticket.resolvedAt,
                actionable = ticket.status != CsTicketStatus.COMPLETED
            )
    }
}

data class AdminCsTicketUserResponse(
    val userId: Long?,
    val nickname: String?,
    val email: String?,
    val phoneNumber: String?,
)

data class AdminCsTicketOwnerResponse(
    val storeId: Long,
    val storeName: String,
    val storePhoneNumber: String?,
)

data class AdminCsTicketGroupBuyResponse(
    val groupBuyId: Long,
    val productName: String,
    val storeName: String,
)

data class AdminCsTicketUpdateRequest(
    val status: CsTicketStatus? = null,

    @field:Size(max = 50)
    val assigneeName: String? = null,

    @field:Size(max = 1000)
    val processingMemo: String? = null,
)

private fun CsTicket.calculateSlaHours(now: LocalDateTime): Long {
    val startedAt = createdAt ?: return 0L
    val endedAt = if (status == CsTicketStatus.COMPLETED) {
        resolvedAt ?: now
    } else {
        now
    }

    return Duration.between(startedAt, endedAt).toHours().coerceAtLeast(0)
}
