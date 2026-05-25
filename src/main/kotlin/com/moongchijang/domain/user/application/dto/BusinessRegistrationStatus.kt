package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사업자등록번호 조회 상태")
enum class BusinessRegistrationStatus {
    VALID,
    CLOSED,
    NOT_FOUND,
}
