package com.moongchijang.domain.auth.infrastructure.email.store

import com.moongchijang.domain.auth.application.port.EmailSignupTokenStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisEmailSignupTokenStore(
    private val redisTemplate: StringRedisTemplate
) : EmailSignupTokenStore {

    override fun save(email: String, signupToken: String, expiresInSeconds: Long) {
        redisTemplate.opsForValue().set(tokenKey(email), signupToken, Duration.ofSeconds(expiresInSeconds))
    }

    override fun isValid(email: String, signupToken: String): Boolean {
        val saved = redisTemplate.opsForValue().get(tokenKey(email)) ?: return false
        return saved == signupToken
    }

    override fun delete(email: String) {
        redisTemplate.delete(tokenKey(email))
    }

    private fun tokenKey(email: String): String = "auth:email:signup-token:$email"
}
