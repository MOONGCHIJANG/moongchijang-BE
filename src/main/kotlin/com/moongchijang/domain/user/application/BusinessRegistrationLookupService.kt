package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.application.dto.BusinessRegistrationLookupRequest
import com.moongchijang.domain.user.application.dto.BusinessRegistrationLookupResponse
import com.moongchijang.domain.user.application.dto.BusinessRegistrationStatus
import com.moongchijang.domain.user.application.port.BusinessRegistrationLookupPort
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BusinessRegistrationLookupService(
    private val businessRegistrationLookupPort: BusinessRegistrationLookupPort,
) {
    private val log = LoggerFactory.getLogger(BusinessRegistrationLookupService::class.java)

    fun lookup(request: BusinessRegistrationLookupRequest): BusinessRegistrationLookupResponse {
        val businessRegistrationNumber = normalize(request.businessRegistrationNumber)
        log.info(
            "[BusinessRegistrationLookupService] 사업자등록 조회 시작: registrationNumberSuffix={}",
            businessRegistrationNumber.takeLast(4),
        )
        validateBusinessRegistrationNumberFormat(businessRegistrationNumber)

        val result = businessRegistrationLookupPort.lookup(businessRegistrationNumber)

        val response = BusinessRegistrationLookupResponse(
            businessRegistrationNumber = format(businessRegistrationNumber),
            status = result.status,
            message = result.status.notFoundMessage(),
            storeName = result.storeName?.trim()?.takeIf { it.isNotBlank() },
            ownerName = result.ownerName?.trim()?.takeIf { it.isNotBlank() },
            storeAddress = result.storeAddress?.trim()?.takeIf { it.isNotBlank() },
        )
        log.info(
            "[BusinessRegistrationLookupService] 사업자등록 조회 완료: registrationNumberSuffix={}, status={}",
            businessRegistrationNumber.takeLast(4),
            response.status,
        )
        return response
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

private fun BusinessRegistrationStatus.notFoundMessage(): String? {
    return when (this) {
        BusinessRegistrationStatus.NOT_FOUND -> "사업자등록번호가 조회되지 않아 다시 입력해주세요."
        else -> null
    }
}
