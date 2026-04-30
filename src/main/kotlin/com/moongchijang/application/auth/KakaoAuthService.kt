package com.moongchijang.application.auth

import com.moongchijang.application.auth.dto.KakaoAuthUser
import com.moongchijang.infrastructure.oauth.kakao.client.KakaoAuthClient
import com.moongchijang.infrastructure.oauth.kakao.client.KakaoUserInfoClient
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KakaoAuthService(
    private val kakaoAuthClient: KakaoAuthClient,
    private val kakaoUserInfoClient: KakaoUserInfoClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getKakaoUser(authorizationCode: String): KakaoAuthUser {
        log.info("[KakaoAuthService] 카카오 사용자 조회 시작")
        val kakaoAccessToken = kakaoAuthClient.getAccessToken(authorizationCode)
        val userInfo = kakaoUserInfoClient.getUserInfo(kakaoAccessToken)

        val providerId = userInfo.id?.toString()
            ?: throw CustomException(ErrorCode.KAKAO_USER_INFO_INVALID)

        val email = userInfo.kakaoAccount?.email
            ?.takeIf { it.isNotBlank() }
            ?: throw CustomException(ErrorCode.KAKAO_USER_INFO_INVALID)

        val nickname = userInfo.kakaoAccount?.profile?.nickname
            ?.takeIf { it.isNotBlank() }
            ?: throw CustomException(ErrorCode.KAKAO_USER_INFO_INVALID)

        val kakaoUser = KakaoAuthUser(
            providerId = providerId,
            email = email,
            nickname = nickname,
        )
        log.info("[KakaoAuthService] 카카오 사용자 조회 완료: providerId={}", providerId)
        return kakaoUser
    }
}
