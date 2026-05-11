package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    // 활성(미탈퇴) 유저 조회
    fun findByProviderAndProviderIdAndDeletedAtIsNull(
        provider: AuthProvider,
        providerId: String,
    ): User?

    // 탈퇴 유저 조회
    fun findByProviderAndProviderIdAndDeletedAtIsNotNull(
        provider: AuthProvider,
        providerId: String,
    ): User?

    fun findByProviderAndEmailAndDeletedAtIsNull(
        provider: AuthProvider,
        email: String,
    ): User?

    fun existsByNicknameAndDeletedAtIsNull(nickname: String): Boolean

    fun existsByEmailAndDeletedAtIsNull(email: String): Boolean

    fun findByIdAndDeletedAtIsNull(id: Long): User?
}
