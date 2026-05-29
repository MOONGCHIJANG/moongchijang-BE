package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestActionResponse
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestApproveRequest
import com.moongchijang.domain.groupbuy.application.dto.AdminGroupBuyRequestRejectRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyImage
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatusHistory
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyImageRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.domain.store.domain.repository.StoreRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class AdminGroupBuyRequestActionService(
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository,
    private val groupBuyRepository: GroupBuyRepository,
    private val groupBuyImageRepository: GroupBuyImageRepository,
    private val storeRepository: StoreRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
) {
    private val log = LoggerFactory.getLogger(AdminGroupBuyRequestActionService::class.java)

    fun approve(requestId: Long, request: AdminGroupBuyRequestApproveRequest): AdminGroupBuyRequestActionResponse {
        log.info("[AdminGroupBuyRequestActionService] 공구요청 승인 시작: requestId={}", requestId)
        val groupBuyRequest = findRequest(requestId)
        validateActionable(groupBuyRequest)
        val maxQuantity = request.maxQuantity ?: request.targetQuantity
        validateApproveRequest(request, maxQuantity)

        val store = resolveStore(groupBuyRequest, request)
        val images = request.imageUrls.map { s3ImageReferenceResolver.resolve(it) }
        val thumbnail = images.first()

        val groupBuy = groupBuyRepository.save(
            GroupBuy(
                store = store,
                groupBuyRequest = groupBuyRequest,
                thumbnailKey = thumbnail.key,
                productName = request.productName.trim(),
                productDescription = request.productDescription.trim(),
                price = request.price,
                originalPrice = request.originalPrice,
                targetQuantity = request.targetQuantity,
                currentQuantity = 0,
                maxQuantity = maxQuantity,
                perUserLimit = request.perUserLimit,
                status = GroupBuyStatus.IN_PROGRESS,
                recruitmentStartAt = request.recruitmentStartAt,
                deadline = request.deadline,
                pickupDate = request.pickupDate,
                pickupTimeStart = request.pickupTimeStart,
                pickupTimeEnd = request.pickupTimeEnd,
                pickupLocation = request.pickupLocation.trim(),
                pickupContact = request.pickupContact?.trim()?.ifBlank { null },
            )
        )

        groupBuyImageRepository.saveAll(
            images.map {
                val imageKey = it.key
                    ?: throw CustomException(ErrorCode.INVALID_INPUT, "공구 이미지 key가 존재하지 않습니다.")
                GroupBuyImage(
                    groupBuy = groupBuy,
                    imageKey = imageKey,
                )
            }
        )
        markRequest(groupBuyRequest, GroupBuyRequestStatus.OPENED, openedGroupBuyId = groupBuy.id)
        eventPublisher.publishEvent(AdminGroupBuyOpenedEvent(groupBuy.id))

        val response = AdminGroupBuyRequestActionResponse(
            requestId = groupBuyRequest.id,
            status = GroupBuyRequestStatus.OPENED,
            groupBuyId = groupBuy.id
        )
        log.info(
            "[AdminGroupBuyRequestActionService] 공구요청 승인 완료: requestId={}, groupBuyId={}",
            requestId,
            groupBuy.id,
        )
        return response
    }

    fun reject(requestId: Long, request: AdminGroupBuyRequestRejectRequest): AdminGroupBuyRequestActionResponse {
        log.info("[AdminGroupBuyRequestActionService] 공구요청 반려 시작: requestId={}", requestId)
        val groupBuyRequest = findRequest(requestId)
        validateActionable(groupBuyRequest)

        val reason = request.rejectionReason.trim()
        if (reason.isBlank()) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_REJECTION_REASON_REQUIRED)
        }

        markRequest(groupBuyRequest, GroupBuyRequestStatus.REJECTED, rejectionReason = reason)
        val response = AdminGroupBuyRequestActionResponse(
            requestId = groupBuyRequest.id,
            status = GroupBuyRequestStatus.REJECTED,
            groupBuyId = null
        )
        log.info("[AdminGroupBuyRequestActionService] 공구요청 반려 완료: requestId={}", requestId)
        return response
    }

    private fun findRequest(requestId: Long): GroupBuyRequest {
        return groupBuyRequestRepository.findWithLockById(requestId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_REQUEST_NOT_FOUND) }
    }

    private fun validateActionable(groupBuyRequest: GroupBuyRequest) {
        if (groupBuyRequest.status == GroupBuyRequestStatus.OPENED ||
            groupBuyRequest.status == GroupBuyRequestStatus.REJECTED
        ) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_INVALID_STATUS_TRANSITION)
        }
    }

    private fun validateApproveRequest(request: AdminGroupBuyRequestApproveRequest, maxQuantity: Int) {
        if (request.originalPrice != null && request.originalPrice < request.price) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_INVALID_PRICE)
        }
        if (request.targetQuantity > maxQuantity ||
            (request.perUserLimit != null && request.perUserLimit > maxQuantity)
        ) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_INVALID_QUANTITY)
        }
        if (!request.recruitmentStartAt.isBefore(request.deadline)) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_INVALID_PERIOD)
        }
        if (!request.pickupDate.isAfter(request.deadline.toLocalDate()) ||
            !request.pickupTimeStart.isBefore(request.pickupTimeEnd)
        ) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_INVALID_PICKUP)
        }
    }

    private fun resolveStore(
        groupBuyRequest: GroupBuyRequest,
        request: AdminGroupBuyRequestApproveRequest
    ): Store {
        request.storeId?.let { storeId ->
            return storeRepository.findById(storeId)
                .orElseThrow { CustomException(ErrorCode.STORE_NOT_FOUND) }
        }

        val storeName = request.storeName?.trim().takeUnless { it.isNullOrBlank() } ?: groupBuyRequest.storeName
        val storeAddress = request.storeAddress?.trim().takeUnless { it.isNullOrBlank() }
            ?: groupBuyRequest.storeAddress
            ?: throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_STORE_REQUIRED)
        val region = request.region ?: throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_STORE_REQUIRED)
        val district = request.district ?: throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_STORE_REQUIRED)
        if (district.region != region) {
            throw CustomException(ErrorCode.GROUPBUY_REQUEST_APPROVAL_STORE_REGION_MISMATCH)
        }

        storeRepository.findFirstByNameIgnoreCaseAndAddressIgnoreCase(storeName, storeAddress)?.let {
            return it
        }

        return storeRepository.save(
            Store(
                name = storeName,
                address = storeAddress,
                phoneNumber = request.storePhoneNumber?.trim()?.ifBlank { null },
                latitude = request.latitude ?: groupBuyRequest.latitude,
                longitude = request.longitude ?: groupBuyRequest.longitude,
                region = region,
                district = district
            )
        )
    }

    private fun markRequest(
        groupBuyRequest: GroupBuyRequest,
        status: GroupBuyRequestStatus,
        openedGroupBuyId: Long? = null,
        rejectionReason: String? = null
    ) {
        groupBuyRequest.status = status
        groupBuyRequest.openedGroupBuyId = openedGroupBuyId
        groupBuyRequest.rejectionReason = rejectionReason
        groupBuyRequestStatusHistoryRepository.save(
            GroupBuyRequestStatusHistory(
                groupBuyRequestId = groupBuyRequest.id,
                status = status,
                changedAt = LocalDateTime.now()
            )
        )
    }

    data class AdminGroupBuyOpenedEvent(val groupBuyId: Long)
}
