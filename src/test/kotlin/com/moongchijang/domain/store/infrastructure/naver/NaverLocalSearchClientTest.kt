package com.moongchijang.domain.store.infrastructure.naver

import com.moongchijang.global.config.NaverApiProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.ExpectedCount.once
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

class NaverLocalSearchClientTest {

    @Test
    fun `동일 키워드와 display 검색은 성공 응답 캐시를 재사용한다`() {
        val builder = RestClient.builder()
        val server = MockRestServiceServer.bindTo(builder).build()
        val client = NaverLocalSearchClient(
            naverApiProperties = NaverApiProperties(
                clientId = "naver-client-id",
                clientSecret = "naver-client-secret",
                localSearchUrl = "https://naver.test/v1/search/local.json",
            ),
            restClientBuilder = builder,
        )

        server.expect(
            once(),
            requestTo(
                "https://naver.test/v1/search/local.json?query=%EC%84%B1%EC%88%98%20%EC%86%8C%EA%B8%88%EB%B9%B5&display=20"
            )
        )
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("X-Naver-Client-Id", "naver-client-id"))
            .andExpect(header("X-Naver-Client-Secret", "naver-client-secret"))
            .andRespond(
                withSuccess(
                    """
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
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON
                )
            )

        val first = client.search("성수 소금빵", 20)
        val second = client.search("성수 소금빵", 20)

        assertEquals(1, first.total)
        assertEquals(first, second)
        server.verify()
    }
}
