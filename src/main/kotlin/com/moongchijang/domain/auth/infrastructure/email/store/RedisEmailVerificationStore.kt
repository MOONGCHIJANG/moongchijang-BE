package com.moongchijang.domain.auth.infrastructure.email.store

import com.moongchijang.domain.auth.application.port.EmailVerificationStore
import com.moongchijang.global.time.TimePolicy
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate

@Component
class RedisEmailVerificationStore(
    private val redisTemplate: StringRedisTemplate,
) : EmailVerificationStore {

    override fun saveCode(email: String, code: String, expiresInSeconds: Long) {
        redisTemplate.opsForValue().set(codeKey(email), code, Duration.ofSeconds(expiresInSeconds))
    }

    override fun getCode(email: String): String? = redisTemplate.opsForValue().get(codeKey(email))

    override fun deleteCode(email: String) {
        redisTemplate.delete(codeKey(email))
    }

    override fun getResendAvailableInSeconds(email: String): Long {
        val ttl = redisTemplate.getExpire(cooldownKey(email))
        return if (ttl > 0) ttl else 0L
    }

    override fun setResendCooldown(email: String, cooldownSeconds: Long) {
        redisTemplate.opsForValue().set(cooldownKey(email), "1", Duration.ofSeconds(cooldownSeconds))
    }

    override fun incrementDailySendCount(email: String): Long {
        val key = dailyCountKey(email)
        val count = redisTemplate.opsForValue().increment(key) ?: 0L
        if (count == 1L) {
            redisTemplate.expire(key, Duration.ofDays(2))
        }
        return count
    }

    override fun getDailySendCount(email: String): Long {
        return redisTemplate.opsForValue().get(dailyCountKey(email))?.toLongOrNull() ?: 0L
    }

    private fun codeKey(email: String): String = "auth:email:verification:code:$email"
    private fun cooldownKey(email: String): String = "auth:email:verification:cooldown:$email"

    private fun dailyCountKey(email: String): String {
        val today = LocalDate.now(TimePolicy.BUSINESS_ZONE_ID)
        return "auth:email:verification:daily:$today:$email"
    }
}
