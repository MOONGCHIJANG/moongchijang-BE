package com.moongchijang.domain.user.infrastructure.business

import com.moongchijang.domain.user.application.dto.BusinessRegistrationStatus
import com.moongchijang.domain.user.application.port.BusinessRegistrationLookupPort
import com.moongchijang.domain.user.application.port.BusinessRegistrationLookupResult
import com.moongchijang.domain.user.infrastructure.business.dto.NtsBusinessStatusRequest
import com.moongchijang.domain.user.infrastructure.business.dto.NtsBusinessStatusResponse
import com.moongchijang.global.config.BusinessRegistrationApiProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class BusinessRegistrationLookupClient(
    private val properties: BusinessRegistrationApiProperties,
    restClientBuilder: RestClient.Builder,
) : BusinessRegistrationLookupPort {
    private val restClient = restClientBuilder
        .baseUrl(properties.baseUrl)
        .build()

    override fun lookup(businessRegistrationNumber: String): BusinessRegistrationLookupResult {
        if (!properties.enabled || properties.serviceKey.isBlank()) {
            return BusinessRegistrationLookupResult(status = BusinessRegistrationStatus.NOT_FOUND)
        }

        val response = try {
            restClient.post()
                .uri("${properties.statusPath}?serviceKey={serviceKey}&returnType=JSON", properties.serviceKey)
                .body(NtsBusinessStatusRequest(b_no = listOf(businessRegistrationNumber)))
                .retrieve()
                .body(NtsBusinessStatusResponse::class.java)
        } catch (e: RestClientException) {
            null
        }

        if (response == null) {
            return BusinessRegistrationLookupResult(status = BusinessRegistrationStatus.NOT_FOUND)
        }

        val item = response.data.firstOrNull() ?: return BusinessRegistrationLookupResult(
            status = BusinessRegistrationStatus.NOT_FOUND,
        )

        return BusinessRegistrationLookupResult(
            status = mapStatus(item.b_stt_cd, item.b_stt),
            storeName = null,
            ownerName = null,
            storeAddress = null,
        )
    }

    private fun mapStatus(statusCode: String?, statusText: String?): BusinessRegistrationStatus {
        return when {
            statusCode == "01" -> BusinessRegistrationStatus.VALID
            statusCode == "02" || statusCode == "03" -> BusinessRegistrationStatus.CLOSED
            statusText?.contains("계속", ignoreCase = false) == true -> BusinessRegistrationStatus.VALID
            statusText?.contains("휴업", ignoreCase = false) == true -> BusinessRegistrationStatus.CLOSED
            statusText?.contains("폐업", ignoreCase = false) == true -> BusinessRegistrationStatus.CLOSED
            else -> BusinessRegistrationStatus.NOT_FOUND
        }
    }
}
