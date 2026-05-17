package com.moongchijang.domain.groupbuy.domain.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
class RedisGroupBuyViewerCountRepository(
    private val redisTemplate: StringRedisTemplate,
) : GroupBuyViewerCountRepository {

    override fun touch(groupBuyId: Long, viewerKey: String, nowEpochSeconds: Long, ttlSeconds: Long) {
        val key = key(groupBuyId)
        val ops = redisTemplate.opsForZSet()

        pruneExpired(key, nowEpochSeconds, ttlSeconds)
        ops.add(key, viewerKey, nowEpochSeconds.toDouble())
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds * 2))
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
}
