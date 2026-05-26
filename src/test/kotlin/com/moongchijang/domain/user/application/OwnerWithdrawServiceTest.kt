package com.moongchijang.domain.user.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.application.dto.OwnerWithdrawRequest
import com.moongchijang.domain.user.domain.entity.OwnerWithdrawalReason
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

class OwnerWithdrawServiceTest {

    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val storeStaffRepository: StoreStaffRepository = Mockito.mock(StoreStaffRepository::class.java)
    private val groupBuyRepository: GroupBuyRepository = Mockito.mock(GroupBuyRepository::class.java)
    private val participationRepository: ParticipationRepository = Mockito.mock(ParticipationRepository::class.java)

    private val ownerWithdrawService = OwnerWithdrawService(
        userRepository = userRepository,
        storeStaffRepository = storeStaffRepository,
        groupBuyRepository = groupBuyRepository,
        participationRepository = participationRepository,
    )

    @Test
    fun `사장님 회원탈퇴 개설 공구 존재 예외`() {
        val owner = UserFixture.createKakaoUser(id = 10L, providerId = "owner-10").apply {
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.SELLER))
        }
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(owner)
        Mockito.`when`(storeStaffRepository.findStoreIdsByUserId(10L)).thenReturn(listOf(101L))
        Mockito.`when`(
            groupBuyRepository.existsByStoreIdInAndStatusIn(
                listOf(101L),
                listOf(GroupBuyStatus.IN_PROGRESS),
            )
        ).thenReturn(true)

        val exception = assertThrows<CustomException> {
            ownerWithdrawService.withdraw(
                10L,
                OwnerWithdrawRequest(reason = OwnerWithdrawalReason.INCONVENIENT_SERVICE),
            )
        }

        Assertions.assertEquals(ErrorCode.OWNER_WITHDRAWAL_BLOCKED_OPEN_GROUPBUY, exception.errorCode)
    }

    @Test
    fun `사장님 회원탈퇴 달성 공구 미픽업 존재 예외`() {
        val owner = UserFixture.createKakaoUser(id = 11L, providerId = "owner-11").apply {
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.SELLER))
        }
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(11L)).thenReturn(owner)
        Mockito.`when`(storeStaffRepository.findStoreIdsByUserId(11L)).thenReturn(listOf(201L))
        Mockito.`when`(
            groupBuyRepository.existsByStoreIdInAndStatusIn(
                listOf(201L),
                listOf(GroupBuyStatus.IN_PROGRESS),
            )
        ).thenReturn(false)
        Mockito.`when`(
            participationRepository.existsUnpickedParticipationByStoreIdsAndGroupBuyStatuses(
                storeIds = listOf(201L),
                groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
            )
        ).thenReturn(true)

        val exception = assertThrows<CustomException> {
            ownerWithdrawService.withdraw(
                11L,
                OwnerWithdrawRequest(reason = OwnerWithdrawalReason.NO_LONGER_NEEDED),
            )
        }

        Assertions.assertEquals(ErrorCode.OWNER_WITHDRAWAL_BLOCKED_PENDING_CUSTOMER_PICKUP, exception.errorCode)
    }

    @Test
    fun `사장님 회원탈퇴 기타 사유 상세 미입력 예외`() {
        val owner = UserFixture.createKakaoUser(id = 12L, providerId = "owner-12").apply {
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.SELLER))
        }
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(12L)).thenReturn(owner)

        val exception = assertThrows<CustomException> {
            ownerWithdrawService.withdraw(
                12L,
                OwnerWithdrawRequest(
                    reason = OwnerWithdrawalReason.OTHER,
                    reasonDetail = " ",
                ),
            )
        }

        Assertions.assertEquals(ErrorCode.OWNER_WITHDRAWAL_REASON_DETAIL_REQUIRED, exception.errorCode)
    }

    @Test
    fun `사장님 회원탈퇴 성공`() {
        val owner = UserFixture.createKakaoUser(id = 13L, providerId = "owner-13").apply {
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.SELLER))
        }
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(13L)).thenReturn(owner)
        Mockito.`when`(storeStaffRepository.findStoreIdsByUserId(13L)).thenReturn(listOf(301L))
        Mockito.`when`(
            groupBuyRepository.existsByStoreIdInAndStatusIn(
                listOf(301L),
                listOf(GroupBuyStatus.IN_PROGRESS),
            )
        ).thenReturn(false)
        Mockito.`when`(
            participationRepository.existsUnpickedParticipationByStoreIdsAndGroupBuyStatuses(
                storeIds = listOf(301L),
                groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
                participationStatuses = listOf(ParticipationStatus.CONFIRMED),
            )
        ).thenReturn(false)

        ownerWithdrawService.withdraw(
            13L,
            OwnerWithdrawRequest(
                reason = OwnerWithdrawalReason.PRIVACY_CONCERN,
                reasonDetail = "저장되면 안됨",
            ),
        )

        Mockito.verify(userRepository).save(owner)
        Assertions.assertEquals(OwnerWithdrawalReason.PRIVACY_CONCERN, owner.ownerWithdrawalReason)
        Assertions.assertEquals(null, owner.ownerWithdrawalReasonDetail)
        Assertions.assertEquals(true, owner.deletedAt != null)
    }
}
