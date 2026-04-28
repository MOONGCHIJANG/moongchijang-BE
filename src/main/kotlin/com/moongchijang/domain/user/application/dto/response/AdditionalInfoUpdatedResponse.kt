package com.moongchijang.domain.user.application.dto.response

import com.moongchijang.domain.user.domain.entity.User

data class AdditionalInfoUpdatedResponse(
    val id: Long,
    val nickname: String,
    val phoneNumber: String,
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

