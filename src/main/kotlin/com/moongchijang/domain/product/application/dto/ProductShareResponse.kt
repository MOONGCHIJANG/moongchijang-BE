package com.moongchijang.domain.product.application.dto

data class ProductShareResponse(
    val productId: Long,
    val productName: String,
    val storeName: String,
    val imageUrl: String?,
    val shareUrl: String,
)
