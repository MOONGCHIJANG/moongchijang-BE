package com.moongchijang.domain.groupbuy.domain.entity

import com.moongchijang.support.GroupBuyFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class GroupBuyTest {

    @Test
    fun `IN_PROGRESS 상태일 때 ACHIEVED 전이`() {
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 1L,
            status = GroupBuyStatus.IN_PROGRESS
        )

        groupBuy.transitionToAchieved()

        assertEquals(GroupBuyStatus.ACHIEVED, groupBuy.status)
    }

    @Test
    fun `ACHIEVED 외 상태일 때 ACHIEVED 전이 불가`() {
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 1L,
            status = GroupBuyStatus.COMPLETED
        )

        assertThrows(IllegalArgumentException::class.java) {
            groupBuy.transitionToAchieved()
        }
    }

    @Test
    fun `deadline 경과한 IN_PROGRESS 상태일 때 FAILED 전이`() {
        val now = LocalDateTime.now()
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 1L,
            status = GroupBuyStatus.IN_PROGRESS,
            deadline = now.minusMinutes(1)
        )

        groupBuy.transitionToFailedByDeadline(now)

        assertEquals(GroupBuyStatus.FAILED, groupBuy.status)
    }

    @Test
    fun `deadline 경과한 ACHIEVED 상태일 때 COMPLETED 전이`() {
        val now = LocalDateTime.now()
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 1L,
            status = GroupBuyStatus.ACHIEVED,
            deadline = now.minusMinutes(1)
        )

        groupBuy.transitionToCompletedByDeadline(now)

        assertEquals(GroupBuyStatus.COMPLETED, groupBuy.status)
    }

    @Test
    fun `max 수량 달성한 ACHIEVED 상태일 때 COMPLETED 전이`() {
        val groupBuy = GroupBuyFixture.createGroupBuy(
            id = 1L,
            status = GroupBuyStatus.ACHIEVED,
            currentQuantity = 100,
            maxQuantity = 100
        )

        groupBuy.transitionToCompletedWhenMaxQuantityReached()

        assertEquals(GroupBuyStatus.COMPLETED, groupBuy.status)
    }

    @Test
    fun `terminal 상태일 때 true 반환`() {
        val completed = GroupBuyFixture.createGroupBuy(id = 1L, status = GroupBuyStatus.COMPLETED)
        val failed = GroupBuyFixture.createGroupBuy(id = 2L, status = GroupBuyStatus.FAILED)
        val closed = GroupBuyFixture.createGroupBuy(id = 3L, status = GroupBuyStatus.CLOSED)
        val inProgress = GroupBuyFixture.createGroupBuy(id = 4L, status = GroupBuyStatus.IN_PROGRESS)

        assertTrue(completed.isTerminalStatus())
        assertTrue(failed.isTerminalStatus())
        assertTrue(closed.isTerminalStatus())
        assertFalse(inProgress.isTerminalStatus())
    }
}
