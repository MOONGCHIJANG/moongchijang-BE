package com.moongchijang.domain.user.infrastructure.business

import com.moongchijang.domain.user.application.dto.BusinessRegistrationStatus
import com.moongchijang.domain.user.application.port.BusinessRegistrationLookupPort
import com.moongchijang.domain.user.application.port.BusinessRegistrationLookupResult
import com.moongchijang.domain.user.infrastructure.business.dto.BusinessRegistrationApiResponse
import com.moongchijang.global.config.BusinessRegistrationApiProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class BusinessRegistrationLookupClient(
    private val properties: BusinessRegistrationApiProperties,
    restClientBuilder: RestClient.Builder,
) : BusinessRegistrationLookupPort {
    private val restClient = restClientBuilder.build()

    override fun lookup(businessRegistrationNumber: String): BusinessRegistrationLookupResult {
        if (!properties.enabled || properties.url.isBlank()) {
            return BusinessRegistrationLookupResult(status = BusinessRegistrationStatus.NOT_FOUND)
        }

        val response = try {
            restClient.get()
                .uri("${properties.url}?b_no={businessRegistrationNumber}", businessRegistrationNumber)
                .header("X-Service-Key", properties.serviceKey)
                .retrieve()
                .body(BusinessRegistrationApiResponse::class.java)
        } catch (e: RestClientException) {
            null
        }

        if (response == null) {
            return BusinessRegistrationLookupResult(status = BusinessRegistrationStatus.NOT_FOUND)
        }

        return BusinessRegistrationLookupResult(
            status = mapStatus(response.status),
            storeName = response.storeName,
            ownerName = response.ownerName,
            storeAddress = response.storeAddress,
        )
    }

    private fun mapStatus(status: String?): BusinessRegistrationStatus {
        return when (status?.trim()?.uppercase()) {
            "VALID", "ACTIVE" -> BusinessRegistrationStatus.VALID
            "CLOSED", "INACTIVE" -> BusinessRegistrationStatus.CLOSED
            else -> BusinessRegistrationStatus.NOT_FOUND
        }
    }
}
