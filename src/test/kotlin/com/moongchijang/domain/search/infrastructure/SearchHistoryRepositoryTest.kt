package com.moongchijang.domain.search.infrastructure

import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.data.redis.core.ListOperations
import org.springframework.data.redis.core.StringRedisTemplate

class SearchHistoryRepositoryTest {

    private val redisTemplate: StringRedisTemplate = Mockito.mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    private val listOperations: ListOperations<String, String> =
        Mockito.mock(ListOperations::class.java) as ListOperations<String, String>

    private val repository = SearchHistoryRepository(redisTemplate)

    @Test
    fun `save removes duplicated keyword before pushing latest keyword`() {
        Mockito.`when`(redisTemplate.opsForList()).thenReturn(listOperations)

        repository.save(userId = 1L, query = "소금빵")

        val inOrder = Mockito.inOrder(listOperations)
        inOrder.verify(listOperations).remove("search:history:1", 0, "소금빵")
        inOrder.verify(listOperations).leftPush("search:history:1", "소금빵")
        inOrder.verify(listOperations).trim("search:history:1", 0, 9)
    }
}
