package com.moongchijang.domain.search.infrastructure

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript

class SearchHistoryRepositoryTest {

    private val redisTemplate: StringRedisTemplate = Mockito.mock(StringRedisTemplate::class.java)
    private val repository = SearchHistoryRepository(redisTemplate)

    @Test
    fun `save updates history with atomic redis script`() {
        repository.save(userId = 1L, query = "소금빵")

        @Suppress("UNCHECKED_CAST")
        Mockito.verify(redisTemplate).execute(
            anyRedisScript(),
            eqValue(listOf("search:history:1")),
            eqValue("소금빵"),
            eqValue("10"),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun anyRedisScript(): RedisScript<Long> {
        any(RedisScript::class.java)
        return DefaultRedisScript("return 1", Long::class.java) as RedisScript<Long>
    }

    private fun <T> eqValue(value: T): T {
        eq(value)
        return value
    }
}
