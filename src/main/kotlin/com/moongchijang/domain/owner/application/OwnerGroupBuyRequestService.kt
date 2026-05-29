package com.moongchijang.domain.owner.application

import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateRequest
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestCreateResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestDetailResponse
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyRequestPageResponse
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequest
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestImage
import com.moongchijang.domain.owner.domain.entity.OwnerGroupBuyRequestStatus
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestImageRepository
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestRepository
import com.moongchijang.domain.store.domain.repository.StoreRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
@Transactional
class OwnerGroupBuyRequestService(
    private val userRepository: UserRepository,
    private val storeRepository: StoreRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val ownerGroupBuyRequestRepository: OwnerGroupBuyRequestRepository,
    private val ownerGroupBuyRequestImageRepository: OwnerGroupBuyRequestImageRepository,
    private val clock: Clock
) {
    private val log = LoggerFactory.getLogger(OwnerGroupBuyRequestService::class.java)

    @Transactional(readOnly = true)
    fun getMyRequests(ownerId: Long, pageable: Pageable): OwnerGroupBuyRequestPageResponse {
        log.info(
            "[OwnerGroupBuyRequestService] 사장님 요청공구 목록 조회 시작: ownerId={}, page={}, size={}",
            ownerId,
            pageable.pageNumber,
            pageable.pageSize,
        )
        validateSeller(ownerId)

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            val emptyResponse = OwnerGroupBuyRequestPageResponse.from(PageImpl(emptyList(), pageable, 0))
            log.info("[OwnerGroupBuyRequestService] 사장님 요청공구 목록 조회 완료(소속 매장 없음): ownerId={}", ownerId)
            return emptyResponse
        }

        val page: Page<OwnerGroupBuyRequest> = ownerGroupBuyRequestRepository
            .findPageByOwnerIdAndStoreIdIn(ownerId, storeIds, pageable)
        val response = OwnerGroupBuyRequestPageResponse.from(page)
        log.info(
            "[OwnerGroupBuyRequestService] 사장님 요청공구 목록 조회 완료: ownerId={}, totalElements={}",
            ownerId,
            response.totalElements,
        )
        return response
    }

    @Transactional(readOnly = true)
    fun getDetail(ownerId: Long, requestId: Long): OwnerGroupBuyRequestDetailResponse {
        log.info("[OwnerGroupBuyRequestService] 사장님 요청공구 상세 조회 시작: ownerId={}, requestId={}", ownerId, requestId)
        validateSeller(ownerId)

        val request = ownerGroupBuyRequestRepository.findById(requestId)
            .orElseThrow { CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_NOT_FOUND) }

        if (request.owner.id != ownerId || !storeStaffRepository.existsByUserIdAndStoreId(ownerId, request.store.id)) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val images = ownerGroupBuyRequestImageRepository.findAllByRequestIdOrderBySortOrderAsc(requestId)
        val response = OwnerGroupBuyRequestDetailResponse.from(request, images)
        log.info("[OwnerGroupBuyRequestService] 사장님 요청공구 상세 조회 완료: ownerId={}, requestId={}", ownerId, requestId)
        return response
    }

    fun create(ownerId: Long, request: OwnerGroupBuyRequestCreateRequest): OwnerGroupBuyRequestCreateResponse {
        log.info("[OwnerGroupBuyRequestService] 사장님 요청공구 생성 시작: ownerId={}, storeId={}", ownerId, request.storeId)
        val owner = validateSeller(ownerId)

        val store = storeRepository.findById(request.storeId)
            .orElseThrow { CustomException(ErrorCode.STORE_NOT_FOUND) }

        if (!storeStaffRepository.existsByUserIdAndStoreId(ownerId, request.storeId)) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        validateRequest(request)

        val saved = ownerGroupBuyRequestRepository.save(
            OwnerGroupBuyRequest(
                owner = owner,
                store = store,
                productName = request.productName.trim(),
                productDescription = request.productDescription.trim(),
                originalPrice = request.originalPrice,
                price = request.price,
                targetQuantity = request.targetQuantity,
                maxQuantity = request.maxQuantity,
                perUserLimit = request.perUserLimit,
                thumbnailUrl = request.imageUrls.first().trim(),
                deadline = request.deadline,
                pickupDate = request.pickupDate,
                pickupTimeStart = request.pickupTimeStart,
                pickupTimeEnd = request.pickupTimeEnd,
                pickupLocation = request.pickupLocation.trim(),
                pickupContact = request.pickupContact?.trim()?.ifBlank { null },
                status = OwnerGroupBuyRequestStatus.PENDING
            )
        )

        ownerGroupBuyRequestImageRepository.saveAll(
            request.imageUrls.mapIndexed { index, imageUrl ->
                OwnerGroupBuyRequestImage(
                    request = saved,
                    imageUrl = imageUrl.trim(),
                    sortOrder = index
                )
            }
        )

        val response = OwnerGroupBuyRequestCreateResponse(
            requestId = saved.id,
            status = saved.status
        )
        log.info(
            "[OwnerGroupBuyRequestService] 사장님 요청공구 생성 완료: ownerId={}, requestId={}",
            ownerId,
            saved.id,
        )
        return response
    }

    private fun validateSeller(ownerId: Long) =
        userRepository.findByIdAndDeletedAtIsNull(ownerId)
            ?.also {
                if (it.role != UserRole.SELLER) {
                    throw CustomException(ErrorCode.FORBIDDEN)
                }
            }
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

    private fun validateRequest(request: OwnerGroupBuyRequestCreateRequest) {
        if (request.deadline.isBefore(LocalDateTime.now(clock).plusDays(MIN_RECRUITING_DAYS))) {
            throw CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_DEADLINE)
        }

        if (request.maxQuantity < request.targetQuantity) {
            throw CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_QUANTITY)
        }

        if (request.perUserLimit != null && request.perUserLimit > request.maxQuantity) {
            throw CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_QUANTITY)
        }

        if (!request.pickupTimeEnd.isAfter(request.pickupTimeStart)) {
            throw CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_PICKUP_TIME)
        }

        if (!request.pickupDate.isAfter(request.deadline.toLocalDate())) {
            throw CustomException(ErrorCode.OWNER_GROUPBUY_REQUEST_INVALID_PICKUP_DATE)
        }
    }

    private companion object {
        const val MIN_RECRUITING_DAYS = 7L
    }
}
