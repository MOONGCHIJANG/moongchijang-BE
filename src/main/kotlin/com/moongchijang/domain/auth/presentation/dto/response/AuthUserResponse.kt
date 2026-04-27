package com.moongchijang.domain.auth.presentation.dto.response

import com.moongchijang.domain.user.domain.entity.AuthProvider
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
)
