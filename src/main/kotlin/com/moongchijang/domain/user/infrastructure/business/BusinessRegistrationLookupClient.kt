package com.moongchijang.domain.user.infrastructure.business

import com.moongchijang.domain.user.application.dto.BusinessRegistrationStatus
import com.moongchijang.domain.user.application.port.BusinessRegistrationLookupPort
import com.moongchijang.domain.user.application.port.BusinessRegistrationLookupResult
import com.moongchijang.domain.user.infrastructure.business.dto.NtsBusinessStatusRequest
import com.moongchijang.domain.user.infrastructure.business.dto.NtsBusinessStatusResponse
import com.moongchijang.global.config.BusinessRegistrationApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class BusinessRegistrationLookupClient(
    private val properties: BusinessRegistrationApiProperties,
    restClientBuilder: RestClient.Builder,
) : BusinessRegistrationLookupPort {
    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = restClientBuilder
        .baseUrl(properties.baseUrl)
        .build()

    override fun lookup(businessRegistrationNumber: String): BusinessRegistrationLookupResult {
        if (!properties.enabled || properties.serviceKey.isBlank()) {
            return BusinessRegistrationLookupResult(status = BusinessRegistrationStatus.NOT_FOUND)
        }

        val response = try {
            restClient.post()
                .uri { builder ->
                    builder
                        .path(properties.statusPath)
                        .queryParam("serviceKey", properties.serviceKey)
                        .queryParam("returnType", "JSON")
                        .build()
                }
                .body(NtsBusinessStatusRequest(b_no = listOf(businessRegistrationNumber)))
                .retrieve()
                .body(NtsBusinessStatusResponse::class.java)
        } catch (e: Exception) {
            log.warn(
                "[BusinessRegistrationLookupClient] 사업자 상태조회 호출 실패: bNo={}, message={}",
                businessRegistrationNumber,
                e.message,
            )
            null
        }

        if (response == null) {
            return BusinessRegistrationLookupResult(status = BusinessRegistrationStatus.NOT_FOUND)
        }

        val item = response.data.firstOrNull() ?: return BusinessRegistrationLookupResult(
            status = BusinessRegistrationStatus.NOT_FOUND,
        )

        return BusinessRegistrationLookupResult(
            status = mapStatus(item.b_stt_cd),
            storeName = null,
            ownerName = null,
            storeAddress = null,
        )
    }

    private fun mapStatus(statusCode: String?): BusinessRegistrationStatus {
        return when {
            statusCode == "01" -> BusinessRegistrationStatus.VALID
            statusCode == "02" || statusCode == "03" -> BusinessRegistrationStatus.CLOSED
            else -> BusinessRegistrationStatus.NOT_FOUND
        }
    }
}
