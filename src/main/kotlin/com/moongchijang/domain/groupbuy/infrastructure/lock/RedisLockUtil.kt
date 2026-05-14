package com.moongchijang.domain.groupbuy.infrastructure.lock

import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.Collections
import java.util.UUID

@Component
class RedisLockUtil(
    private val redisTemplate: StringRedisTemplate
) {
    companion object {
        private const val RETRY_INTERVAL_MS = 50L
    }

    fun lockKey(groupBuyId: Long): String = "groupBuy:$groupBuyId"

    fun tryLockOrThrow(key: String, waitMs: Long, leaseMs: Long): String {
        val token = UUID.randomUUID().toString()
        val deadline = System.currentTimeMillis() + waitMs

        while (System.currentTimeMillis() < deadline) {
            val locked = redisTemplate.opsForValue()
                .setIfAbsent(key, token, Duration.ofMillis(leaseMs))

            if (locked == true) return token

            try {
                Thread.sleep(RETRY_INTERVAL_MS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw CustomException(ErrorCode.GROUPBUY_LOCK_INTERRUPTED)
            }
        }

        throw CustomException(ErrorCode.GROUPBUY_LOCK_ACQUISITION_FAILED)
    }

    fun unlock(key: String, token: String): Boolean {
        val script = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            else
              return 0
            end
        """.trimIndent()

        val result = redisTemplate.execute(
            DefaultRedisScript(script, Long::class.java),
            Collections.singletonList(key),
            token
        )

        return result == 1L
    }
}
