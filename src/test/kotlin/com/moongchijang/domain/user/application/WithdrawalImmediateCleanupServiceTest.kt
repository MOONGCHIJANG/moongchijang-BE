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
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class WithdrawalImmediateCleanupServiceTest {

    private val sellerBusinessProfileRepository: SellerBusinessProfileRepository =
        Mockito.mock(SellerBusinessProfileRepository::class.java)
    private val sellerSettlementAccountRepository: SellerSettlementAccountRepository =
        Mockito.mock(SellerSettlementAccountRepository::class.java)
    private val storeStaffRepository: StoreStaffRepository =
        Mockito.mock(StoreStaffRepository::class.java)
    private val notificationRepository: NotificationRepository =
        Mockito.mock(NotificationRepository::class.java)
    private val notificationDispatchHistoryRepository: NotificationDispatchHistoryRepository =
        Mockito.mock(NotificationDispatchHistoryRepository::class.java)
    private val searchHistoryRepository: SearchHistoryRepository =
        Mockito.mock(SearchHistoryRepository::class.java)
    private val groupBuyRequestRepository: GroupBuyRequestRepository =
        Mockito.mock(GroupBuyRequestRepository::class.java)
    private val groupBuyRequestStatusHistoryRepository: GroupBuyRequestStatusHistoryRepository =
        Mockito.mock(GroupBuyRequestStatusHistoryRepository::class.java)
    private val groupBuyOpenRequestRepository: GroupBuyOpenRequestRepository =
        Mockito.mock(GroupBuyOpenRequestRepository::class.java)
    private val ownerGroupBuyRequestImageRepository: OwnerGroupBuyRequestImageRepository =
        Mockito.mock(OwnerGroupBuyRequestImageRepository::class.java)
    private val ownerGroupBuyRequestRepository: OwnerGroupBuyRequestRepository =
        Mockito.mock(OwnerGroupBuyRequestRepository::class.java)

    private val service = WithdrawalImmediateCleanupService(
        sellerBusinessProfileRepository = sellerBusinessProfileRepository,
        sellerSettlementAccountRepository = sellerSettlementAccountRepository,
        storeStaffRepository = storeStaffRepository,
        notificationRepository = notificationRepository,
        notificationDispatchHistoryRepository = notificationDispatchHistoryRepository,
        searchHistoryRepository = searchHistoryRepository,
        groupBuyRequestRepository = groupBuyRequestRepository,
        groupBuyRequestStatusHistoryRepository = groupBuyRequestStatusHistoryRepository,
        groupBuyOpenRequestRepository = groupBuyOpenRequestRepository,
        ownerGroupBuyRequestImageRepository = ownerGroupBuyRequestImageRepository,
        ownerGroupBuyRequestRepository = ownerGroupBuyRequestRepository,
    )

    @Test
    fun `탈퇴 즉시 파기 대상 데이터를 모두 정리한다`() {
        val userId = 123L
        Mockito.`when`(notificationRepository.deleteByUserId(userId)).thenReturn(5L)
        Mockito.`when`(notificationDispatchHistoryRepository.deleteByUserId(userId)).thenReturn(4L)
        Mockito.`when`(groupBuyRequestStatusHistoryRepository.deleteByGroupBuyRequest_User_Id(userId)).thenReturn(3L)
        Mockito.`when`(groupBuyRequestRepository.deleteByUser_Id(userId)).thenReturn(2L)
        Mockito.`when`(groupBuyOpenRequestRepository.deleteByUser_Id(userId)).thenReturn(1L)
        Mockito.`when`(ownerGroupBuyRequestImageRepository.deleteByRequest_Owner_Id(userId)).thenReturn(6L)
        Mockito.`when`(ownerGroupBuyRequestRepository.deleteByOwnerId(userId)).thenReturn(7L)
        Mockito.`when`(storeStaffRepository.deleteByUserId(userId)).thenReturn(8L)
        Mockito.`when`(sellerSettlementAccountRepository.deleteByUserId(userId)).thenReturn(9L)
        Mockito.`when`(sellerBusinessProfileRepository.deleteByUserId(userId)).thenReturn(10L)

        service.cleanup(userId)

        Mockito.verify(notificationRepository).deleteByUserId(userId)
        Mockito.verify(notificationDispatchHistoryRepository).deleteByUserId(userId)
        Mockito.verify(groupBuyRequestStatusHistoryRepository).deleteByGroupBuyRequest_User_Id(userId)
        Mockito.verify(groupBuyRequestRepository).deleteByUser_Id(userId)
        Mockito.verify(groupBuyOpenRequestRepository).deleteByUser_Id(userId)
        Mockito.verify(ownerGroupBuyRequestImageRepository).deleteByRequest_Owner_Id(userId)
        Mockito.verify(ownerGroupBuyRequestRepository).deleteByOwnerId(userId)
        Mockito.verify(storeStaffRepository).deleteByUserId(userId)
        Mockito.verify(sellerSettlementAccountRepository).deleteByUserId(userId)
        Mockito.verify(sellerBusinessProfileRepository).deleteByUserId(userId)
        Mockito.verify(searchHistoryRepository).clear(userId)
    }
}
