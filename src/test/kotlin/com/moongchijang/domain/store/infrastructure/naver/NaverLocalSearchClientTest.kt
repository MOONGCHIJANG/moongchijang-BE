package com.moongchijang.domain.store.infrastructure.naver

import com.moongchijang.global.config.NaverApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.dao.QueryTimeoutException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

class NaverLocalSearchClientTest {

    private val objectMapper: ObjectMapper = JsonMapper.builder().findAndAddModules().build()
    private val naverApiProperties = NaverApiProperties(
        clientId = "naver-client-id",
        clientSecret = "naver-client-secret",
        localSearchUrl = "https://naver.test/v1/search/local.json",
    )

    private val sampleResponseJson = """
        {
          "total": 1,
          "start": 1,
          "display": 1,
          "items": [
            {
              "title": "<b>성수베이커리</b>",
              "link": "https://map.naver.com/p/entry/place/100",
              "category": "음식점>카페,디저트",
              "description": "",
              "telephone": "",
              "address": "서울 성동구",
              "roadAddress": "서울 성동구 연무장길 1",
              "mapx": "1270559610",
              "mapy": "375445810"
            }
          ]
        }
    """.trimIndent()

    private fun mockedRedis(): Pair<StringRedisTemplate, ValueOperations<String, String>> {
        val redisTemplate = Mockito.mock(StringRedisTemplate::class.java)
        @Suppress("UNCHECKED_CAST")
        val valueOps = Mockito.mock(ValueOperations::class.java) as ValueOperations<String, String>
        Mockito.`when`(redisTemplate.opsForValue()).thenReturn(valueOps)
        return redisTemplate to valueOps
    }

    @Test
    fun `Redis 캐시가 있으면 외부 API를 호출하지 않고 캐시 값을 그대로 반환한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()

        val (redisTemplate, valueOps) = mockedRedis()
        Mockito.`when`(valueOps.get("store:naver-local-search:20:성수 소금빵"))
            .thenReturn(sampleResponseJson)

        val client = NaverLocalSearchClient(naverApiProperties, builder, redisTemplate, objectMapper)

        val result = client.search("성수 소금빵", 20)

        assertEquals(1, result.total)
        assertEquals("https://map.naver.com/p/entry/place/100", result.items.first().link)
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration::class.java))
        server.verify()
    }

    @Test
    fun `캐시 미스 시 외부 API를 호출하고 응답을 Redis에 TTL과 함께 저장한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(
            once(),
            requestTo(
                "https://naver.test/v1/search/local.json?query=%EC%84%B1%EC%88%98%20%EC%86%8C%EA%B8%88%EB%B9%B5&display=20"
            )
        )
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-Naver-Client-Id", "naver-client-id"))
            .andExpect(header("X-Naver-Client-Secret", "naver-client-secret"))
            .andRespond(withSuccess(sampleResponseJson, MediaType.APPLICATION_JSON))

        val (redisTemplate, valueOps) = mockedRedis()
        Mockito.`when`(valueOps.get("store:naver-local-search:20:성수 소금빵")).thenReturn(null)

        val client = NaverLocalSearchClient(naverApiProperties, builder, redisTemplate, objectMapper)

        val result = client.search("성수 소금빵", 20)

        assertEquals(1, result.total)
        verify(valueOps).set(
            eq("store:naver-local-search:20:성수 소금빵"),
            anyString(),
            eq(Duration.ofMinutes(5))
        )
        server.verify()
    }

    @Test
    fun `Redis 조회가 실패해도 외부 API 호출은 정상 동작한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        server.expect(
            once(),
            requestTo(
                "https://naver.test/v1/search/local.json?query=%EC%84%B1%EC%88%98%20%EC%86%8C%EA%B8%88%EB%B9%B5&display=20"
            )
        )
            .andRespond(withSuccess(sampleResponseJson, MediaType.APPLICATION_JSON))

        val (redisTemplate, valueOps) = mockedRedis()
        Mockito.`when`(valueOps.get(anyString())).thenThrow(QueryTimeoutException("redis down"))
        doThrow(QueryTimeoutException("redis down"))
            .`when`(valueOps).set(anyString(), anyString(), any(Duration::class.java))

        val client = NaverLocalSearchClient(naverApiProperties, builder, redisTemplate, objectMapper)

        val result = client.search("성수 소금빵", 20)

        assertEquals(1, result.total)
        server.verify()
    }
}
