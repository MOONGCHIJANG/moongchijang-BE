package com.moongchijang.domain.auth.infrastructure.sms.coolsms.store

import com.moongchijang.domain.auth.application.port.PhoneVerificationStore
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class RedisPhoneVerificationStore(
    private val redisTemplate: StringRedisTemplate
) : PhoneVerificationStore {

    override fun saveCodeHash(phoneNumber: String, codeHash: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(codeKey(phoneNumber), codeHash, Duration.ofSeconds(ttlSeconds))
    }

    override fun getCodeHash(phoneNumber: String): String? {
        return redisTemplate.opsForValue().get(codeKey(phoneNumber))
    }

    override fun deleteCode(phoneNumber: String) {
        redisTemplate.delete(codeKey(phoneNumber))
    }

    override fun markVerified(phoneNumber: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(verifiedKey(phoneNumber), VERIFIED_VALUE, Duration.ofSeconds(ttlSeconds))
    }

    override fun isVerified(phoneNumber: String): Boolean {
        return redisTemplate.opsForValue().get(verifiedKey(phoneNumber)) == VERIFIED_VALUE
    }

    override fun markVerifiedForUser(userId: Long, phoneNumber: String, ttlSeconds: Long) {
        redisTemplate.opsForValue().set(verifiedForUserKey(userId, phoneNumber), VERIFIED_VALUE, Duration.ofSeconds(ttlSeconds))
    }

    override fun isVerifiedForUser(userId: Long, phoneNumber: String): Boolean {
        return redisTemplate.opsForValue().get(verifiedForUserKey(userId, phoneNumber)) == VERIFIED_VALUE
    }

    private fun codeKey(phoneNumber: String): String = "$KEY_PREFIX:code:$phoneNumber"
    private fun verifiedKey(phoneNumber: String): String = "$KEY_PREFIX:verified:$phoneNumber"
    private fun verifiedForUserKey(userId: Long, phoneNumber: String): String = "$KEY_PREFIX:verified:$userId:$phoneNumber"

    companion object {
        private const val KEY_PREFIX = "auth:phone"
        private const val VERIFIED_VALUE = "true"
    }
}
