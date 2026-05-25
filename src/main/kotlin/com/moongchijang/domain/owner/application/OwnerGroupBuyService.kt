package com.moongchijang.domain.owner.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.owner.application.dto.OwnerGroupBuyListItemResponse
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class OwnerGroupBuyService(
    private val userRepository: UserRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val groupBuyRepository: GroupBuyRepository
) {

    fun getMyGroupBuys(ownerId: Long): List<OwnerGroupBuyListItemResponse> {
        val owner = userRepository.findByIdAndDeletedAtIsNull(ownerId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (owner.role != UserRole.SELLER) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        val storeIds = storeStaffRepository.findStoreIdsByUserId(ownerId)
        if (storeIds.isEmpty()) {
            return emptyList()
        }

        return groupBuyRepository
            .findByStoreIdInAndStatusInOrderByDeadlineAsc(storeIds, OWNER_VISIBLE_STATUSES)
            .map { OwnerGroupBuyListItemResponse.from(it) }
    }

    private companion object {
        val OWNER_VISIBLE_STATUSES = listOf(
            GroupBuyStatus.IN_PROGRESS,
            GroupBuyStatus.ACHIEVED,
            GroupBuyStatus.COMPLETED,
            GroupBuyStatus.FAILED,
            GroupBuyStatus.CLOSED
        )
    }
}
