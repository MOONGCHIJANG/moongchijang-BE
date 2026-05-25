package com.moongchijang.domain.user.application

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettlementInstitutionCodeMapperTest {

    @Test
    fun `은행 라벨을 매핑할 때 토스 표준 코드로 변환됨`() {
        val code = SettlementInstitutionCodeMapper.toCode("KB국민")
        assertEquals("KOOKMIN", code)
    }

    @Test
    fun `증권사 라벨을 매핑할 때 토스 표준 코드로 변환됨`() {
        val code = SettlementInstitutionCodeMapper.toCode("카카오페이증권")
        assertEquals("KAKAOPAY_SECURITIES", code)
    }

    @Test
    fun `정의되지 않은 기관 라벨을 매핑할 때 커스텀 코드로 변환됨`() {
        val code = SettlementInstitutionCodeMapper.toCode("우리동네금고")
        assertEquals("UNKNOWN", code)
    }
}
