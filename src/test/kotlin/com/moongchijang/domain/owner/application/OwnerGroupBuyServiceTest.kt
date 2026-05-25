package com.moongchijang.domain.owner.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.GroupBuyFixture
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class OwnerGroupBuyServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var storeStaffRepository: StoreStaffRepository

    @Mock
    private lateinit var groupBuyRepository: GroupBuyRepository

    @InjectMocks
    private lateinit var service: OwnerGroupBuyService

    @Test
    fun `사장님 본인 매장의 공구 목록을 조회한다`() {
        val owner = seller()
        val groupBuy = groupBuy(
            id = 10L,
            status = GroupBuyStatus.IN_PROGRESS,
            currentQuantity = 12,
            targetQuantity = 20,
            price = 9900
        )

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(listOf(1L, 2L))
        `when`(
            groupBuyRepository.findByStoreIdInAndStatusInOrderByDeadlineAsc(
                listOf(1L, 2L),
                listOf(GroupBuyStatus.IN_PROGRESS, GroupBuyStatus.ACHIEVED, GroupBuyStatus.FAILED)
            )
        ).thenReturn(listOf(groupBuy))

        val result = service.getMyGroupBuys(owner.id!!)

        assertEquals(1, result.size)
        assertEquals(10L, result[0].groupBuyId)
        assertEquals("두쫀쿠 1개", result[0].productName)
        assertEquals(20, result[0].targetQuantity)
        assertEquals(12, result[0].currentQuantity)
        assertEquals(60, result[0].achievementRate)
        assertEquals(9900, result[0].price)
        assertEquals(LocalDate.of(2026, 6, 1), result[0].deadline)
        assertEquals("IN_PROGRESS", result[0].status.name)
    }

    @Test
    fun `소속 매장이 없으면 빈 목록을 반환한다`() {
        val owner = seller()

        `when`(userRepository.findByIdAndDeletedAtIsNull(owner.id!!)).thenReturn(owner)
        `when`(storeStaffRepository.findStoreIdsByUserId(owner.id!!)).thenReturn(emptyList())

        val result = service.getMyGroupBuys(owner.id!!)

        assertEquals(emptyList<Any>(), result)
        verifyNoInteractions(groupBuyRepository)
    }

    @Test
    fun `구매자 계정이면 사장님 공구 목록을 조회할 수 없다`() {
        val buyer = UserFixture.createKakaoUser(id = 1L)
        `when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(buyer)

        val ex = assertThrows<CustomException> {
            service.getMyGroupBuys(1L)
        }

        assertEquals(ErrorCode.FORBIDDEN, ex.errorCode)
        verify(storeStaffRepository, never()).findStoreIdsByUserId(1L)
    }

    private fun seller() = UserFixture.createKakaoUser(id = 1L).apply {
        role = UserRole.SELLER
    }

    private fun groupBuy(
        id: Long,
        status: GroupBuyStatus,
        currentQuantity: Int,
        targetQuantity: Int,
        price: Int
    ): GroupBuy =
        GroupBuyFixture.createGroupBuy(
            id = id,
            status = status,
            deadline = LocalDateTime.of(2026, 6, 1, 23, 59),
            currentQuantity = currentQuantity,
            targetQuantity = targetQuantity,
            price = price
        )
}
