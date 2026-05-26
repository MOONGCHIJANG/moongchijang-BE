package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "전화번호 변경 응답")
data class PhoneNumberUpdateResponse(

    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @field:Schema(description = "변경된 전화번호", example = "010-1234-5678")
    val phoneNumber: String,
)
