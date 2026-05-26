package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.application.dto.BusinessRegistrationLookupRequest
import com.moongchijang.domain.user.application.dto.BusinessRegistrationStatus
import com.moongchijang.domain.user.application.port.BusinessRegistrationLookupPort
import com.moongchijang.domain.user.application.port.BusinessRegistrationLookupResult
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito

class BusinessRegistrationLookupServiceTest {
    private val businessRegistrationLookupPort: BusinessRegistrationLookupPort =
        Mockito.mock(BusinessRegistrationLookupPort::class.java)
    private val businessRegistrationLookupService = BusinessRegistrationLookupService(businessRegistrationLookupPort)

    @Test
    fun `사업자등록번호 조회할 때 하이픈이 제거된 번호로 포트 호출됨`() {
        Mockito.`when`(businessRegistrationLookupPort.lookup("1112233333"))
            .thenReturn(BusinessRegistrationLookupResult(status = BusinessRegistrationStatus.VALID))

        val response = businessRegistrationLookupService.lookup(
            BusinessRegistrationLookupRequest(businessRegistrationNumber = "111-22-33333"),
        )

        Mockito.verify(businessRegistrationLookupPort).lookup("1112233333")
        assertEquals("111-22-33333", response.businessRegistrationNumber)
        assertEquals(BusinessRegistrationStatus.VALID, response.status)
    }

    @Test
    fun `사업자등록번호 형식이 올바르지 않을 때 예외가 발생함`() {
        val exception = assertThrows<CustomException> {
            businessRegistrationLookupService.lookup(
                BusinessRegistrationLookupRequest(businessRegistrationNumber = "12345"),
            )
        }

        assertEquals(ErrorCode.INVALID_BUSINESS_REGISTRATION_NUMBER_FORMAT, exception.errorCode)
    }
}
