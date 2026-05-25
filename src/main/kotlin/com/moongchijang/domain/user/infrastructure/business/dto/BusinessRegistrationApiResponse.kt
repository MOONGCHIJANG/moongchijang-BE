package com.moongchijang.domain.user.infrastructure.business.dto

data class BusinessRegistrationApiResponse(
    val status: String? = null,
    val storeName: String? = null,
    val ownerName: String? = null,
    val storeAddress: String? = null,
)
