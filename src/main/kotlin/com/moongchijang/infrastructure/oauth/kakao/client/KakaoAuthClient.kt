package com.moongchijang.infrastructure.oauth.kakao.client

import com.moongchijang.infrastructure.oauth.kakao.config.KakaoProperties
import com.moongchijang.infrastructure.oauth.kakao.dto.KakaoTokenResponse
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder

@Component
class KakaoAuthClient(
    private val restClient: RestClient,
    private val kakaoProperties: KakaoProperties
) {
    fun getAccessToken(authorizationCode: String): String {
        val formBody = UriComponentsBuilder.newInstance()
            .queryParam("grant_type", "authorization_code")
            .queryParam("client_id", kakaoProperties.clientId)
            .queryParam("client_secret", kakaoProperties.clientSecret)
            .queryParam("redirect_uri", kakaoProperties.redirectUri)
            .queryParam("code", authorizationCode)
            .build()
            .query
            ?: throw CustomException(ErrorCode.KAKAO_TOKEN_REQUEST_INVALID)

        return try {
            val response = restClient.post()
                .uri(kakaoProperties.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formBody)
                .retrieve()
                .body<KakaoTokenResponse>()
                ?: throw CustomException(ErrorCode.KAKAO_TOKEN_RESPONSE_INVALID)

            response.accessToken
                ?: throw CustomException(ErrorCode.KAKAO_ACCESS_TOKEN_MISSING)
        } catch (e: RestClientResponseException) {
            throw CustomException(ErrorCode.KAKAO_TOKEN_EXCHANGE_FAILED)
        }
    }
}
