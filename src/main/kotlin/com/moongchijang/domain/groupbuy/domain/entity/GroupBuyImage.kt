package com.moongchijang.domain.groupbuy.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(name = "group_buy_images")
class GroupBuyImage(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_buy_id", nullable = false)
    var groupBuy: GroupBuy,

    @Column(length = 500)
    var imageKey: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L

) : BaseEntity()
