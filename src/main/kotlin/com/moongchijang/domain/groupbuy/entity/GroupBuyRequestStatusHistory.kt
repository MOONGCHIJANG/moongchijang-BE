package com.moongchijang.domain.groupbuy.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "group_buy_request_status_histories")
class GroupBuyRequestStatusHistory(

    @Column(nullable = false)
    val groupBuyRequestId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: GroupBuyRequestStatus,

    @Column(nullable = false)
    val changedAt: LocalDateTime = LocalDateTime.now(),

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
)
