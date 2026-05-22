package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestCreateRequest
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestIdResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyRequestStatusUpdateRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
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
    private val groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val groupBuyOpenRequestService: GroupBuyOpenRequestService,
) {

    fun create(userId: Long, request: GroupBuyRequestCreateRequest): GroupBuyRequestIdResponse {
        if (!request.desiredPickupDate.isAfter(LocalDate.now())) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_INVALID_DATE)
        }

        val placeId = request.placeId?.ifBlank { null }
        val roadAddress = request.roadAddress?.ifBlank { null }
        val lotAddress = request.lotAddress?.ifBlank { null }
        val storeAddress = roadAddress ?: lotAddress ?: request.storeAddress?.ifBlank { null }
        val latitude = if (placeId != null) request.latitude else null
        val longitude = if (placeId != null) request.longitude else null

        val saved = groupBuyRequestRepository.save(
            GroupBuyRequest(
                userId = userId,
                storeName = request.storeName,
                storeAddress = storeAddress,
                placeId = placeId,
                roadAddress = roadAddress,
                lotAddress = lotAddress,
                latitude = latitude,
                longitude = longitude,
                productName = request.productName,
                desiredQuantity = request.desiredQuantity,
                desiredPickupDate = request.desiredPickupDate,
                additionalNote = request.additionalNote,
                contactPhone = request.contactPhone,
                contactInstagram = request.contactInstagram
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

    fun updateStatus(requestId: Long, request: GroupBuyRequestStatusUpdateRequest): GroupBuyRequestResponse {
        val groupBuyRequest = groupBuyRequestRepository.findById(requestId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_REQUEST_NOT_FOUND) }

        validateTransition(groupBuyRequest.status, request.targetStatus)

        val rejectionReason = request.rejectionReason?.trim()
        val openedGroupBuyId = request.openedGroupBuyId
        var openedGroupBuyForNotification: GroupBuy? = null

        when (request.targetStatus) {
            GroupBuyRequestStatus.REJECTED -> {
                if (rejectionReason.isNullOrBlank()) {
                    throw CustomException(ErrorCode.GROUPBUY_REQUEST_REJECTION_REASON_REQUIRED)
                }
                groupBuyRequest.rejectionReason = rejectionReason
                groupBuyRequest.openedGroupBuyId = null
            }
            GroupBuyRequestStatus.OPENED -> {
                openedGroupBuyForNotification = openedGroupBuyId?.let {
                    groupBuyRepository.findWithStoreById(it)
                        .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }
                }
                groupBuyRequest.rejectionReason = null
                groupBuyRequest.openedGroupBuyId = openedGroupBuyId
            }
            GroupBuyRequestStatus.IN_CONTACT -> {
                groupBuyRequest.rejectionReason = null
                groupBuyRequest.openedGroupBuyId = null
            }
            GroupBuyRequestStatus.IN_REVIEW -> {
                throw CustomException(ErrorCode.GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION)
            }
        }

        groupBuyRequest.status = request.targetStatus
        groupBuyRequestStatusHistoryRepository.save(
            GroupBuyRequestStatusHistory(
                groupBuyRequestId = groupBuyRequest.id,
                status = request.targetStatus
            )
        )
        openedGroupBuyForNotification?.let { groupBuyOpenRequestService.notifyOpened(it) }

        val history = groupBuyRequestStatusHistoryRepository
            .findByGroupBuyRequestIdOrderByChangedAtAsc(requestId)

        return GroupBuyRequestResponse.from(groupBuyRequest, history)
    }

    private fun validateTransition(
        currentStatus: GroupBuyRequestStatus,
        targetStatus: GroupBuyRequestStatus
    ) {
        val isAllowed = when (currentStatus) {
            GroupBuyRequestStatus.IN_REVIEW -> targetStatus == GroupBuyRequestStatus.IN_CONTACT
            GroupBuyRequestStatus.IN_CONTACT ->
                targetStatus == GroupBuyRequestStatus.OPENED || targetStatus == GroupBuyRequestStatus.REJECTED
            GroupBuyRequestStatus.OPENED,
            GroupBuyRequestStatus.REJECTED -> false
        }

        if (!isAllowed) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION)
        }
    }
}
