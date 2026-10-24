package com.moongchijang.domain.product.domain.repository

import com.moongchijang.domain.product.domain.entity.ProductImage
import org.springframework.data.jpa.repository.JpaRepository

interface ProductImageRepository : JpaRepository<ProductImage, Long> {
    fun findAllByProductIdOrderBySortOrderAscIdAsc(productId: Long): List<ProductImage>
}
