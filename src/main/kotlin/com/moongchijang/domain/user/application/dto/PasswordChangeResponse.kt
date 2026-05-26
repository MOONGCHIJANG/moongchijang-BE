package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "비밀번호 변경 응답")
data class PasswordChangeResponse(

    @field:Schema(description = "비밀번호 변경 완료 여부", example = "true")
    val changed: Boolean,
)
