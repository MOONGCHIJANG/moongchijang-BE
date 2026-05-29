package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestDetailResponse
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestPageResponse
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestStatusFilter
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
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class GroupBuyRequestService(
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val groupBuyOpenRequestService: GroupBuyOpenRequestService,
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(GroupBuyRequestService::class.java)

    fun create(userId: Long, request: GroupBuyRequestCreateRequest): GroupBuyRequestIdResponse {
        log.info("[GroupBuyRequestService] 공구요청 생성 시작: userId={}", userId)
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

        val response = GroupBuyRequestIdResponse(saved.id)
        log.info("[GroupBuyRequestService] 공구요청 생성 완료: userId={}, requestId={}", userId, saved.id)
        return response
    }

    @Transactional(readOnly = true)
    fun getMyRequests(userId: Long): List<GroupBuyRequestResponse> {
        log.info("[GroupBuyRequestService] 내 공구요청 목록 조회 시작: userId={}", userId)
        val requests = groupBuyRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
        if (requests.isEmpty()) return emptyList()

        val historyMap = groupBuyRequestStatusHistoryRepository
            .findByGroupBuyRequestIdInOrderByChangedAtAsc(requests.map { it.id })
            .groupBy { it.groupBuyRequestId }

        val responses = requests.map { GroupBuyRequestResponse.from(it, historyMap[it.id] ?: emptyList()) }
        log.info("[GroupBuyRequestService] 내 공구요청 목록 조회 완료: userId={}, count={}", userId, responses.size)
        return responses
    }

    @Transactional(readOnly = true)
    fun getDetail(userId: Long, requestId: Long): GroupBuyRequestResponse {
        log.info("[GroupBuyRequestService] 공구요청 상세 조회 시작: userId={}, requestId={}", userId, requestId)
        val request = groupBuyRequestRepository.findById(requestId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_REQUEST_NOT_FOUND) }

        if (request.userId != userId) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_FORBIDDEN)
        }

        val history = groupBuyRequestStatusHistoryRepository
            .findByGroupBuyRequestIdOrderByChangedAtAsc(requestId)

        val response = GroupBuyRequestResponse.from(request, history)
        log.info("[GroupBuyRequestService] 공구요청 상세 조회 완료: userId={}, requestId={}", userId, requestId)
        return response
    }

    @Transactional(readOnly = true)
    fun getAdminRequests(
        status: AdminGroupBuyRequestStatusFilter,
        keyword: String?,
        pageable: Pageable
    ): AdminGroupBuyRequestPageResponse {
        log.info(
            "[GroupBuyRequestService] 관리자 공구요청 목록 조회 시작: status={}, page={}, size={}",
            status,
            pageable.pageNumber,
            pageable.pageSize,
        )
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }
        val requestIdKeyword = normalizedKeyword?.toLongOrNull()
        val page = groupBuyRequestRepository.searchAdminRequests(
            status = status.toStatus(),
            keyword = normalizedKeyword,
            requestIdKeyword = requestIdKeyword,
            pageable = pageable
        )

        val userIds = page.content.map { it.userId }.distinct()
        val usersById = if (userIds.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findAllById(userIds)
                .mapNotNull { user -> user.id?.let { it to user } }
                .toMap()
        }
        val groupBuysById = findOpenedGroupBuys(page.content)

        val response = AdminGroupBuyRequestPageResponse.from(page, usersById, groupBuysById, LocalDateTime.now(clock))
        log.info(
            "[GroupBuyRequestService] 관리자 공구요청 목록 조회 완료: status={}, totalElements={}",
            status,
            response.totalElements,
        )
        return response
    }

    @Transactional(readOnly = true)
    fun getAdminDetail(requestId: Long): AdminGroupBuyRequestDetailResponse {
        log.info("[GroupBuyRequestService] 관리자 공구요청 상세 조회 시작: requestId={}", requestId)
        val request = groupBuyRequestRepository.findById(requestId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_REQUEST_NOT_FOUND) }
        val requester = userRepository.findById(request.userId).orElse(null)
        val history = groupBuyRequestStatusHistoryRepository
            .findByGroupBuyRequestIdOrderByChangedAtAsc(requestId)

        val response = AdminGroupBuyRequestDetailResponse.from(request, requester, history)
        log.info("[GroupBuyRequestService] 관리자 공구요청 상세 조회 완료: requestId={}", requestId)
        return response
    }

    fun updateStatus(requestId: Long, request: GroupBuyRequestStatusUpdateRequest): GroupBuyRequestResponse {
        log.info(
            "[GroupBuyRequestService] 공구요청 상태 변경 시작: requestId={}, targetStatus={}",
            requestId,
            request.targetStatus,
        )
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
                val openedId = openedGroupBuyId
                    ?: throw CustomException(ErrorCode.GROUPBUY_REQUEST_OPENED_GROUP_BUY_REQUIRED)
                openedGroupBuyForNotification = groupBuyRepository.findWithStoreById(openedId)
                    .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }
                groupBuyRequest.rejectionReason = null
                groupBuyRequest.openedGroupBuyId = openedId
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
                status = request.targetStatus,
                changedAt = LocalDateTime.now()
            )
        )
        openedGroupBuyForNotification?.let { notifyOpenedAfterCommit(it) }

        val history = groupBuyRequestStatusHistoryRepository
            .findByGroupBuyRequestIdOrderByChangedAtAsc(requestId)

        val response = GroupBuyRequestResponse.from(groupBuyRequest, history)
        log.info(
            "[GroupBuyRequestService] 공구요청 상태 변경 완료: requestId={}, status={}",
            requestId,
            response.status,
        )
        return response
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

    private fun notifyOpenedAfterCommit(groupBuy: GroupBuy) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            groupBuyOpenRequestService.notifyOpened(groupBuy)
            return
        }

        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    groupBuyOpenRequestService.notifyOpened(groupBuy)
                }
            }
        )
    }

    private fun findOpenedGroupBuys(requests: List<GroupBuyRequest>): Map<Long, GroupBuy> {
        val openedGroupBuyIds = requests.mapNotNull { it.openedGroupBuyId }.distinct()
        if (openedGroupBuyIds.isEmpty()) {
            return emptyMap()
        }

        return groupBuyRepository.findAllById(openedGroupBuyIds).associateBy { it.id }
    }
}
