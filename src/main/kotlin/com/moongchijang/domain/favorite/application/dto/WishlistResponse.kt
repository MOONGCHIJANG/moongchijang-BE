package com.moongchijang.domain.favorite.application.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class WishlistPageResponse(
    val content: List<WishlistItemResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val urgentCount: Int,
)

data class WishlistItemResponse(
    val groupBuyId: Long,
    val thumbnailUrl: String,
    val dDay: Int,
    val storeName: String,
    val regionLabel: String,
    val productName: String,
    val pickupDate: LocalDate,
    val deadline: LocalDateTime,
    val achievementRate: Int,
    val price: Int,
    val currentParticipantCount: Int,
    val targetParticipantCount: Int,
    val isWishlisted: Boolean,
)
