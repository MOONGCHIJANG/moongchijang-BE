package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "닉네임 변경 응답")
data class NicknameUpdateResponse(

    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @field:Schema(description = "변경된 닉네임", example = "문치장")
    val nickname: String,
)
