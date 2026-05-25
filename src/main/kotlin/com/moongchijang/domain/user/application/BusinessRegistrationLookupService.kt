package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.application.dto.BusinessRegistrationLookupRequest
import com.moongchijang.domain.user.application.dto.BusinessRegistrationLookupResponse
import com.moongchijang.domain.user.application.port.BusinessRegistrationLookupPort
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Service

@Service
class BusinessRegistrationLookupService(
    private val businessRegistrationLookupPort: BusinessRegistrationLookupPort,
) {
    fun lookup(request: BusinessRegistrationLookupRequest): BusinessRegistrationLookupResponse {
        val businessRegistrationNumber = normalize(request.businessRegistrationNumber)
        validateBusinessRegistrationNumberFormat(businessRegistrationNumber)

        val result = businessRegistrationLookupPort.lookup(businessRegistrationNumber)

        return BusinessRegistrationLookupResponse(
            businessRegistrationNumber = format(businessRegistrationNumber),
            status = result.status,
            storeName = result.storeName?.trim()?.takeIf { it.isNotBlank() },
            ownerName = result.ownerName?.trim()?.takeIf { it.isNotBlank() },
            storeAddress = result.storeAddress?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    private fun normalize(raw: String): String = raw.replace(Regex("[^0-9]"), "")

    private fun validateBusinessRegistrationNumberFormat(businessRegistrationNumber: String) {
        if (!BUSINESS_REGISTRATION_NUMBER_REGEX.matches(businessRegistrationNumber)) {
            throw CustomException(ErrorCode.INVALID_BUSINESS_REGISTRATION_NUMBER_FORMAT)
        }
    }

    private fun format(businessRegistrationNumber: String): String {
        return businessRegistrationNumber.replace(BUSINESS_REGISTRATION_NUMBER_CAPTURE_REGEX, "$1-$2-$3")
    }

    companion object {
        private val BUSINESS_REGISTRATION_NUMBER_REGEX = Regex("^\\d{10}$")
        private val BUSINESS_REGISTRATION_NUMBER_CAPTURE_REGEX = Regex("^(\\d{3})(\\d{2})(\\d{5})$")
    }
}
