package com.moongchijang.domain.owner.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "owner_group_buy_request_images",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_owner_gbr_images_request_sort",
            columnNames = ["request_id", "sort_order"]
        )
    ],
    indexes = [
        Index(name = "idx_owner_gbr_images_request_id", columnList = "request_id")
    ]
)
class OwnerGroupBuyRequestImage(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    var request: OwnerGroupBuyRequest,

    @Column(name = "image_key", nullable = false, length = 500)
    var imageKey: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
) : BaseEntity()
