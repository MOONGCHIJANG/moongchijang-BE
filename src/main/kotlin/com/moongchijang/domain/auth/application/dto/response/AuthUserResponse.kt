package com.moongchijang.domain.auth.application.dto.response

import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import java.time.LocalDateTime

data class AuthUserResponse(
    val id: Long,
    val provider: AuthProvider,
    val providerId: String?,
    val email: String?,
    val nickname: String?,
    val phoneNumber: String?,
    val role: UserRole,
    val signupCompleted: Boolean,
    val deletedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(user: User): AuthUserResponse = AuthUserResponse(
            id = user.id!!,
            provider = user.provider,
            providerId = user.providerId,
            email = user.email,
            nickname = user.nickname,
            phoneNumber = user.phoneNumber,
            role = user.role,
            signupCompleted = user.signupCompleted,
            deletedAt = user.deletedAt,
            createdAt = user.createdAt!!,
            updatedAt = user.updatedAt!!,
        )
    }
}
