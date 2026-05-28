package com.moongchijang.domain.user.application.port

import com.moongchijang.domain.user.application.dto.BusinessRegistrationStatus

interface BusinessRegistrationLookupPort {
    fun lookup(businessRegistrationNumber: String): BusinessRegistrationLookupResult
}

data class BusinessRegistrationLookupResult(
    val status: BusinessRegistrationStatus,
    val storeName: String? = null,
    val ownerName: String? = null,
    val storeAddress: String? = null,
)
