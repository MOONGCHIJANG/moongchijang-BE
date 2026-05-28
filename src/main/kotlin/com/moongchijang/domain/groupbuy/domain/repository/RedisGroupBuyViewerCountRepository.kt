package com.moongchijang.domain.groupbuy.domain.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisGroupBuyViewerCountRepository(
    private val redisTemplate: StringRedisTemplate,
) : GroupBuyViewerCountRepository {

    override fun touchAndCount(groupBuyId: Long, viewerKey: String, nowEpochSeconds: Long, ttlSeconds: Long): Long {
        val key = key(groupBuyId)
        val expireBefore = (nowEpochSeconds - ttlSeconds).toString()
        val keyTtlSeconds = (ttlSeconds * 2).toString()

        val count = redisTemplate.execute(
            TOUCH_AND_COUNT_SCRIPT,
            listOf(key),
            expireBefore,
            viewerKey,
            nowEpochSeconds.toString(),
            keyTtlSeconds
        )
        return count ?: 0L
    }

    override fun countActive(groupBuyId: Long, nowEpochSeconds: Long, ttlSeconds: Long): Long {
        val key = key(groupBuyId)
        val ops = redisTemplate.opsForZSet()

        pruneExpired(key, nowEpochSeconds, ttlSeconds)
        return ops.zCard(key) ?: 0L
    }

    private fun pruneExpired(key: String, nowEpochSeconds: Long, ttlSeconds: Long) {
        val expireBefore = (nowEpochSeconds - ttlSeconds).toDouble()
        redisTemplate.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, expireBefore)
    }

    private fun key(groupBuyId: Long): String = "groupBuy:viewers:$groupBuyId"

    companion object {
        private val TOUCH_AND_COUNT_SCRIPT = DefaultRedisScript<Long>().apply {
            resultType = Long::class.java
            setScriptText("""
                redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
                redis.call('ZADD', KEYS[1], ARGV[3], ARGV[2])
                redis.call('EXPIRE', KEYS[1], ARGV[4])
                return redis.call('ZCARD', KEYS[1])
            """.trimIndent())
        }
    }
}
