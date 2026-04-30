package com.moongchijang.application.user.dto.response

import com.moongchijang.domain.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "추가정보 입력 완료 응답")
data class AdditionalInfoUpdatedResponse(

    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @field:Schema(description = "저장된 닉네임", example = "문치장")
    val nickname: String,

    @field:Schema(description = "저장된 전화번호", example = "010-1234-5678")
    val phoneNumber: String,

    @field:Schema(description = "추가정보 입력 완료 여부", example = "true")
    val signupCompleted: Boolean,
) {
    companion object {
        fun from(user: User): AdditionalInfoUpdatedResponse {
            val userId = user.id ?: error("user id must not be null")
            val nickname = user.nickname ?: error("nickname must not be null")
            val phoneNumber = user.phoneNumber ?: error("phoneNumber must not be null")

            return AdditionalInfoUpdatedResponse(
                id = userId,
                nickname = nickname,
                phoneNumber = phoneNumber,
                signupCompleted = user.signupCompleted,
            )
        }
    }
}
