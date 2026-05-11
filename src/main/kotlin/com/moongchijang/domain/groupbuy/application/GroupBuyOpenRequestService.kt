package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.CreateGroupBuyOpenRequestRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOpenRequest
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyOpenRequestRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class GroupBuyOpenRequestService(
    private val openRequestRepository: GroupBuyOpenRequestRepository
) {
    fun create(userId: Long, request: CreateGroupBuyOpenRequestRequest) {
        if (openRequestRepository.existsByUserIdAndRegionAndProductName(userId, request.region, request.productName)) {
            throw CustomException(ErrorCode.DUPLICATE_OPEN_REQUEST)
        }
        try {
            openRequestRepository.saveAndFlush(
                GroupBuyOpenRequest(
                    userId = userId,
                    region = request.region,
                    productName = request.productName
                )
            )
        } catch (e: DataIntegrityViolationException) {
            throw CustomException(ErrorCode.DUPLICATE_OPEN_REQUEST)
        }
    }
}
