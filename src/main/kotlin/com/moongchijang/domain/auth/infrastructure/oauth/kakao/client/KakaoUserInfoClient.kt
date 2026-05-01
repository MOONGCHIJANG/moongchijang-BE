package com.moongchijang.domain.auth.infrastructure.oauth.kakao.client

import com.moongchijang.global.config.KakaoProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.domain.auth.infrastructure.oauth.kakao.dto.KakaoUserInfoResponse
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.body

@Component
class KakaoUserInfoClient(
    private val restClient: RestClient,
    private val kakaoProperties: KakaoProperties,
) {
    fun getUserInfo(accessToken: String): KakaoUserInfoResponse {
        return try {
            val response = restClient.get()
                .uri(kakaoProperties.userInfoUri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body<KakaoUserInfoResponse>()
                ?: throw CustomException(ErrorCode.KAKAO_USER_INFO_INVALID)

            val email = response.kakaoAccount?.email
            val nickname = response.kakaoAccount?.profile?.nickname

            if (response.id == null || email.isNullOrBlank() || nickname.isNullOrBlank()) {
                throw CustomException(ErrorCode.KAKAO_USER_INFO_INVALID)
            }

            response
        } catch (e: RestClientResponseException) {
            throw CustomException(ErrorCode.KAKAO_USER_INFO_FETCH_FAILED)
        }
    }
}
