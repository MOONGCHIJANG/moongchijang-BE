package com.moongchijang.domain.product.domain.repository

import com.moongchijang.domain.product.domain.entity.Product

interface ProductRepositoryCustom {
    fun findFeed(
        criteria: ProductFeedCriteria,
        cursor: ProductFeedCursor?,
        limit: Int,
    ): List<Product>
}
