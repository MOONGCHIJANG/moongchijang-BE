package com.moongchijang.domain.auth.application

import com.moongchijang.domain.auth.application.dto.KakaoAuthUser
import com.moongchijang.domain.auth.infrastructure.oauth.kakao.client.KakaoAuthClient
import com.moongchijang.domain.auth.infrastructure.oauth.kakao.client.KakaoUserInfoClient
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KakaoAuthService(
    private val kakaoAuthClient: KakaoAuthClient,
    private val kakaoUserInfoClient: KakaoUserInfoClient,
) {

    @Transactional(readOnly = true)
    fun getKakaoUser(authorizationCode: String): KakaoAuthUser {
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

        return KakaoAuthUser(
            providerId = providerId,
            email = email,
            nickname = nickname,
        )
    }
}
