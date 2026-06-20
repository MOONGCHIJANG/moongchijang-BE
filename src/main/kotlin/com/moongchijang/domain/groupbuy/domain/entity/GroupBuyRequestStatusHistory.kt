package com.moongchijang.domain.groupbuy.domain.entity

import com.moongchijang.global.time.TimePolicy
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "group_buy_request_status_histories")
class GroupBuyRequestStatusHistory(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_request_id", nullable = false)
    val groupBuyRequest: GroupBuyRequest,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: GroupBuyRequestStatus,

    @Column(nullable = false)
    val changedAt: LocalDateTime = LocalDateTime.now(TimePolicy.STORAGE_ZONE_ID),

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
)
