package com.moongchijang.domain.owner.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyImage
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyImageRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyRequestActionResponse
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyRequestDetailResponse
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyRequestPageResponse
import com.moongchijang.domain.owner.application.dto.AdminOwnerGroupBuyRequestRejectRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestImageRepository
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
class AdminOwnerGroupBuyRequestService(
    private val ownerGroupBuyRequestRepository: OwnerGroupBuyRequestRepository,
    private val ownerGroupBuyRequestImageRepository: OwnerGroupBuyRequestImageRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val groupBuyImageRepository: GroupBuyImageRepository,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(AdminOwnerGroupBuyRequestService::class.java)

    @Transactional(readOnly = true)
    fun getRequests(
        status: OwnerGroupBuyRequestStatus?,
        keyword: String?,
        pageable: Pageable
    ): AdminOwnerGroupBuyRequestPageResponse {
        val normalizedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }
        log.info(
            "[AdminOwnerGroupBuyRequestService] 사장님 공구 요청 목록 조회 시작: status={}, keyword={}, page={}, size={}",
            status,
            normalizedKeyword,
            pageable.pageNumber,
            pageable.pageSize,
        )
        val page = ownerGroupBuyRequestRepository.searchAdminRequests(status, normalizedKeyword, pageable)
        val response = AdminOwnerGroupBuyRequestPageResponse.from(page, LocalDateTime.now(clock))
        log.info(
            "[AdminOwnerGroupBuyRequestService] 사장님 공구 요청 목록 조회 완료: totalElements={}",
            response.totalElements,
        )
        return response
    }

    @Transactional(readOnly = true)
    fun getDetail(requestId: Long): AdminOwnerGroupBuyRequestDetailResponse {
        log.info("[AdminOwnerGroupBuyRequestService] 사장님 공구 요청 상세 조회 시작: requestId={}", requestId)
        val request = ownerGroupBuyRequestRepository.findAdminDetailById(requestId)
            .orElseThrow { CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_NOT_FOUND) }
        val images = ownerGroupBuyRequestImageRepository.findAllByRequestIdOrderBySortOrderAsc(requestId)
        val response = AdminOwnerGroupBuyRequestDetailResponse.from(
            request = request,
            images = images,
            imageUrls = images.map { s3ImageReferenceResolver.resolveForRead(it.imageKey).orEmpty() },
        )
        log.info("[AdminOwnerGroupBuyRequestService] 사장님 공구 요청 상세 조회 완료: requestId={}", requestId)
        return response
    }

    fun approve(requestId: Long): AdminOwnerGroupBuyRequestActionResponse {
        log.info("[AdminOwnerGroupBuyRequestService] 사장님 공구 요청 승인 시작: requestId={}", requestId)
        val request = findRequestForAction(requestId)
        validatePending(request)
        validateApprovalSource(request)

        val now = LocalDateTime.now(clock)
        val groupBuy = groupBuyRepository.save(
            GroupBuy(
                store = request.store,
                groupBuyRequest = null,
                thumbnailKey = request.thumbnailKey,
                productName = request.productName.trim(),
                productDescription = request.productDescription.trim(),
                price = request.price,
                originalPrice = request.originalPrice,
                targetQuantity = request.targetQuantity,
                currentQuantity = 0,
                maxQuantity = request.maxQuantity,
                perUserLimit = request.perUserLimit,
                status = GroupBuyStatus.IN_PROGRESS,
                recruitmentStartAt = now,
                deadline = request.deadline,
                pickupDate = request.pickupDate,
                pickupTimeStart = request.pickupTimeStart,
                pickupTimeEnd = request.pickupTimeEnd,
                pickupLocation = request.pickupLocation.trim(),
                pickupContact = request.pickupContact?.trim()?.ifBlank { null },
            )
        )

        val images = ownerGroupBuyRequestImageRepository.findAllByRequestIdOrderBySortOrderAsc(requestId)
        groupBuyImageRepository.saveAll(
            images.map {
                GroupBuyImage(
                    groupBuy = groupBuy,
                    imageKey = it.imageKey,
                )
            }
        )

        request.status = OwnerGroupBuyRequestStatus.APPROVED
        request.approvedGroupBuy = groupBuy
        request.rejectionReason = null
        request.reviewedAt = now

        val response = AdminOwnerGroupBuyRequestActionResponse(
            requestId = request.id,
            status = request.status,
            groupBuyId = groupBuy.id
        )
        log.info(
            "[AdminOwnerGroupBuyRequestService] 사장님 공구 요청 승인 완료: requestId={}, groupBuyId={}",
            requestId,
            groupBuy.id,
        )
        return response
    }

    fun reject(requestId: Long, rejectRequest: AdminOwnerGroupBuyRequestRejectRequest): AdminOwnerGroupBuyRequestActionResponse {
        log.info("[AdminOwnerGroupBuyRequestService] 사장님 공구 요청 반려 시작: requestId={}", requestId)
        val request = findRequestForAction(requestId)
        validatePending(request)

        val reason = rejectRequest.rejectionReason.trim()
        if (reason.isBlank()) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_REJECTION_REASON_REQUIRED)
        }

        request.status = OwnerGroupBuyRequestStatus.REJECTED
        request.rejectionReason = reason
        request.reviewedAt = LocalDateTime.now(clock)
        request.approvedGroupBuy = null

        val response = AdminOwnerGroupBuyRequestActionResponse(
            requestId = request.id,
            status = request.status,
            groupBuyId = null
        )
        log.info("[AdminOwnerGroupBuyRequestService] 사장님 공구 요청 반려 완료: requestId={}", requestId)
        return response
    }

    private fun findRequestForAction(requestId: Long): OwnerGroupBuyRequest =
        ownerGroupBuyRequestRepository.findWithLockById(requestId)
            .orElseThrow { CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_NOT_FOUND) }

    private fun validatePending(request: OwnerGroupBuyRequest) {
        if (request.status != OwnerGroupBuyRequestStatus.PENDING) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION)
        }
    }

    private fun validateApprovalSource(request: OwnerGroupBuyRequest) {
        val originalPrice = request.originalPrice
        if (originalPrice != null && originalPrice < request.price) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_INVALID_PRICE)
        }
        val perUserLimit = request.perUserLimit
        if (request.targetQuantity > request.maxQuantity ||
            (perUserLimit != null && perUserLimit > request.maxQuantity)
        ) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_INVALID_QUANTITY)
        }
        if (!request.pickupDate.isAfter(request.deadline.toLocalDate()) ||
            !request.pickupTimeStart.isBefore(request.pickupTimeEnd)
        ) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_INVALID_PICKUP)
        }
    }
}
