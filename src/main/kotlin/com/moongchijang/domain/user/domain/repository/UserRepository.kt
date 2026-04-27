package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): User?

    fun existsByNickname(nickname: String): Boolean

    fun existsByEmail(email: String): Boolean
}
