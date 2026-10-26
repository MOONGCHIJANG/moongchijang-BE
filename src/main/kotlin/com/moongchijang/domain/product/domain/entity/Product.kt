package com.moongchijang.domain.product.domain.entity

import com.moongchijang.domain.store.domain.entity.Store
import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "products")
class Product(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @Column(name = "category_code", nullable = false, length = 50)
    var categoryCode: String = DEFAULT_CATEGORY_CODE,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(name = "unit_price", nullable = false)
    var unitPrice: Int,

    @Column(name = "min_quantity", nullable = false)
    var minQuantity: Int,

    @Column(name = "max_quantity", nullable = false)
    var maxQuantity: Int,

    @Column(name = "preparation_days", nullable = false)
    var preparationDays: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_type", nullable = false, length = 30)
    var storageType: ProductStorageType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: ProductStatus = ProductStatus.ACTIVE,

    @Column(name = "thumbnail_key", nullable = false, length = 500)
    var thumbnailKey: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity() {
    init {
        require(categoryCode.isNotBlank()) { "categoryCode must not be blank" }
        categoryCode = categoryCode.trim().uppercase()
    }

    companion object {
        const val DEFAULT_CATEGORY_CODE = "ETC"
    }
}
