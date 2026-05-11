package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestCreateRequest
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestIdResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestResponse
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class GroupBuyRequestService(
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository
) {

    fun create(userId: Long, request: GroupBuyRequestCreateRequest): GroupBuyRequestIdResponse {
        if (!request.desiredPickupDate.isAfter(LocalDate.now())) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_INVALID_DATE)
        }

        val saved = groupBuyRequestRepository.save(
            GroupBuyRequest(
                userId = userId,
                storeName = request.storeName,
                storeAddress = request.storeAddress,
                productName = request.productName,
                desiredQuantity = request.desiredQuantity,
                desiredPickupDate = request.desiredPickupDate,
                additionalNote = request.additionalNote
            )
        )

        groupBuyRequestStatusHistoryRepository.save(
            GroupBuyRequestStatusHistory(
                groupBuyRequestId = saved.id,
                status = GroupBuyRequestStatus.IN_REVIEW,
                changedAt = saved.createdAt ?: LocalDateTime.now()
            )
        )

        return GroupBuyRequestIdResponse(saved.id)
    }

    @Transactional(readOnly = true)
    fun getMyRequests(userId: Long): List<GroupBuyRequestResponse> {
        val requests = groupBuyRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
        if (requests.isEmpty()) return emptyList()

        val historyMap = groupBuyRequestStatusHistoryRepository
            .findByGroupBuyRequestIdInOrderByChangedAtAsc(requests.map { it.id })
            .groupBy { it.groupBuyRequestId }

        return requests.map { GroupBuyRequestResponse.from(it, historyMap[it.id] ?: emptyList()) }
    }

    @Transactional(readOnly = true)
    fun getDetail(userId: Long, requestId: Long): GroupBuyRequestResponse {
        val request = groupBuyRequestRepository.findById(requestId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_REQUEST_NOT_FOUND) }

        if (request.userId != userId) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_FORBIDDEN)
        }

        val history = groupBuyRequestStatusHistoryRepository
            .findByGroupBuyRequestIdOrderByChangedAtAsc(requestId)

        return GroupBuyRequestResponse.from(request, history)
    }
}
