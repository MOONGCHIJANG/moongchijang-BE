package com.moongchijang.domain.search.infrastructure

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class SearchHistoryRepository(
    private val redisTemplate: StringRedisTemplate
) {
    companion object {
        private const val MAX_HISTORY = 10L
        private fun key(userId: Long) = "search:history:$userId"
    }

    fun save(userId: Long, query: String) {
        val ops = redisTemplate.opsForList()
        val key = key(userId)
        ops.leftPush(key, query)
        ops.trim(key, 0, MAX_HISTORY - 1)
    }

    fun getHistory(userId: Long): List<String> =
        redisTemplate.opsForList().range(key(userId), 0, MAX_HISTORY - 1) ?: emptyList()

    fun clear(userId: Long) {
        redisTemplate.delete(key(userId))
    }

    fun deleteOne(userId: Long, keyword: String) {
        redisTemplate.opsForList().remove(key(userId), 1, keyword)
    }
}
