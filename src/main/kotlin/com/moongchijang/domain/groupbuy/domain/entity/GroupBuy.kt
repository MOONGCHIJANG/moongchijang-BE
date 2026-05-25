package com.moongchijang.domain.groupbuy.domain.entity

import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "group_buys")
class GroupBuy(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_request_id", nullable = false)
    var groupBuyRequest: GroupBuyRequest,

    @Column(length = 500)
    var thumbnailUrl: String? = null,

    @Column(nullable = false, length = 100)
    var productName: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var productDescription: String,

    @Column(nullable = false)
    var price: Int,

    @Column(name = "original_price")
    var originalPrice: Int? = null,

    @Column(nullable = false)
    var targetQuantity: Int,

    @Column(nullable = false)
    var currentQuantity: Int = 0,

    @Column(nullable = false)
    var maxQuantity: Int,

    @Column(name = "per_user_limit")
    var perUserLimit: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GroupBuyStatus,

    @Column(name = "recruitment_start_at")
    var recruitmentStartAt: LocalDateTime? = null,

    @Column(nullable = false)
    var deadline: LocalDateTime,

    @Column(nullable = false)
    var pickupDate: LocalDate,

    @Column(nullable = false)
    var pickupTimeStart: LocalTime,

    @Column(nullable = false)
    var pickupTimeEnd: LocalTime,

    @Column(nullable = false, length = 200)
    var pickupLocation: String,

    @Column(name = "pickup_contact", length = 20)
    var pickupContact: String? = null,

    @Column(nullable = false)
    var shareCount: Int = 0,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L

) : BaseEntity()
{
    fun transitionToAchieved() {
        requireStatus(GroupBuyStatus.IN_PROGRESS)
        status = GroupBuyStatus.ACHIEVED
    }

    fun transitionToCompletedByDeadline(now: LocalDateTime) {
        requireStatus(GroupBuyStatus.ACHIEVED)
        require(deadline <= now) { "deadline 이 지나지 않아 COMPLETED 로 전이할 수 없습니다." }
        status = GroupBuyStatus.COMPLETED
    }

    fun transitionToFailedByDeadline(now: LocalDateTime) {
        requireStatus(GroupBuyStatus.IN_PROGRESS)
        require(deadline <= now) { "deadline 이 지나지 않아 FAILED 로 전이할 수 없습니다." }
        status = GroupBuyStatus.FAILED
    }

    fun transitionToCompletedWhenMaxQuantityReached() {
        requireStatus(GroupBuyStatus.ACHIEVED)
        require(currentQuantity >= maxQuantity) { "max 수량 미달성 상태에서는 COMPLETED 로 전이할 수 없습니다." }
        status = GroupBuyStatus.COMPLETED
    }

    fun transitionToClosed() {
        if (status == GroupBuyStatus.CLOSED) {
            return
        }
        require(status == GroupBuyStatus.IN_PROGRESS || status == GroupBuyStatus.ACHIEVED) {
            "CLOSED 는 IN_PROGRESS 또는 ACHIEVED 상태에서만 전이할 수 있습니다."
        }
        status = GroupBuyStatus.CLOSED
    }

    fun isTerminalStatus(): Boolean {
        return status == GroupBuyStatus.COMPLETED || status == GroupBuyStatus.FAILED || status == GroupBuyStatus.CLOSED
    }

    private fun requireStatus(expected: GroupBuyStatus) {
        require(status == expected) { "${expected.name} 전이 조건을 만족하지 않습니다. 현재 상태: ${status.name}" }
    }
}
