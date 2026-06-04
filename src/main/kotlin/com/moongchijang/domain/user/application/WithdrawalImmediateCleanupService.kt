package com.moongchijang.domain.user.application

import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyOpenRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRequestStatusHistoryRepository
import com.moongchijang.domain.notification.domain.repository.NotificationDispatchHistoryRepository
import com.moongchijang.domain.notification.domain.repository.NotificationRepository
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestImageRepository
import com.moongchijang.domain.owner.domain.repository.OwnerGroupBuyRequestRepository
import com.moongchijang.domain.search.infrastructure.SearchHistoryRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.repository.SellerBusinessProfileRepository
import com.moongchijang.domain.user.domain.repository.SellerSettlementAccountRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WithdrawalImmediateCleanupService(
    private val sellerBusinessProfileRepository: SellerBusinessProfileRepository,
    private val sellerSettlementAccountRepository: SellerSettlementAccountRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationDispatchHistoryRepository: NotificationDispatchHistoryRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val groupBuyRequestRepository: GroupBuyRequestRepository,
    private val groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository,
    private val groupBuyOpenRequestRepository: GroupBuyOpenRequestRepository,
    private val ownerGroupBuyRequestImageRepository: OwnerGroupBuyRequestImageRepository,
    private val ownerGroupBuyRequestRepository: OwnerGroupBuyRequestRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun cleanup(userId: Long) {
        val deletedNotificationCount = notificationRepository.deleteByUserId(userId)
        val deletedDispatchHistoryCount = notificationDispatchHistoryRepository.deleteByUserId(userId)
        val deletedGroupBuyRequestHistoryCount = groupBuyRequestStatusHistoryRepository.deleteByGroupBuyRequest_User_Id(userId)
        val deletedGroupBuyRequestCount = groupBuyRequestRepository.deleteByUser_Id(userId)
        val deletedGroupBuyOpenRequestCount = groupBuyOpenRequestRepository.deleteByUser_Id(userId)
        val deletedOwnerRequestImageCount = ownerGroupBuyRequestImageRepository.deleteByRequest_Owner_Id(userId)
        val deletedOwnerRequestCount = ownerGroupBuyRequestRepository.deleteByOwnerId(userId)
        val deletedStoreStaffCount = storeStaffRepository.deleteByUserId(userId)
        val deletedSellerSettlementCount = sellerSettlementAccountRepository.deleteByUserId(userId)
        val deletedSellerBusinessCount = sellerBusinessProfileRepository.deleteByUserId(userId)
        searchHistoryRepository.clear(userId)

        log.info(
            "[WithdrawalImmediateCleanupService] 탈퇴 즉시파기 데이터 정리 완료: userId={}, notifications={}, dispatchHistories={}, groupBuyRequestHistories={}, groupBuyRequests={}, groupBuyOpenRequests={}, ownerRequestImages={}, ownerRequests={}, storeStaffs={}, sellerSettlementAccounts={}, sellerBusinessProfiles={}",
            userId,
            deletedNotificationCount,
            deletedDispatchHistoryCount,
            deletedGroupBuyRequestHistoryCount,
            deletedGroupBuyRequestCount,
            deletedGroupBuyOpenRequestCount,
            deletedOwnerRequestImageCount,
            deletedOwnerRequestCount,
            deletedStoreStaffCount,
            deletedSellerSettlementCount,
            deletedSellerBusinessCount,
        )
    }
}
