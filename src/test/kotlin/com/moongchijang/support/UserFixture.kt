package com.moongchijang.support

import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import java.time.LocalDateTime

object UserFixture {

    fun createKakaoUser(
        id: Long = 1L,
        providerId: String = "kakao-1",
        email: String = "test@example.com",
        nickname: String = "테스트유저",
        deletedAt: LocalDateTime? = null,
    ): User {
        return User(
            id = id,
            provider = AuthProvider.KAKAO,
            providerId = providerId,
            email = email,
            nickname = nickname,
            role = UserRole.BUYER,
            signupCompleted = false,
            deletedAt = deletedAt,
        )
    }

    fun createEmailUser(
        id: Long = 2L,
        email: String = "email@example.com",
        passwordHash: String = "hashed-password",
        nickname: String? = null,
        deletedAt: LocalDateTime? = null,
    ): User {
        return User(
            id = id,
            provider = AuthProvider.EMAIL,
            providerId = null,
            email = email,
            passwordHash = passwordHash,
            nickname = nickname,
            role = UserRole.BUYER,
            signupCompleted = false,
            deletedAt = deletedAt,
        )
    }
}
