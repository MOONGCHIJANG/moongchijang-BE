package com.moongchijang.domain.auth.application

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class TokenService(
    private val redisTemplate: StringRedisTemplate,
    @Value("\${auth.refresh.expiration-days}") private val refreshExpirationDays: Long,
    @Value("\${auth.refresh.cookie-path:/}") private val refreshCookiePath: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val refreshCookieName = "refreshToken"

    fun issueRefreshToken(userId: Long): String {
        val refreshToken = UUID.randomUUID().toString()
        saveRefreshToken(userId, refreshToken)
        log.info("[TokenService] 리프레시 토큰 발급 완료: userId={}", userId)
        return refreshToken
    }

    fun reissueRefreshToken(userId: Long, oldRefreshToken: String?): String {
        if (!oldRefreshToken.isNullOrBlank()) {
            deleteRefreshToken(oldRefreshToken)
        }
        val newRefreshToken = UUID.randomUUID().toString()
        saveRefreshToken(userId, newRefreshToken)
        log.info("[TokenService] 리프레시 토큰 재발급 완료: userId={}", userId)
        return newRefreshToken
    }

    fun getUserIdByRefreshToken(refreshToken: String): Long? {
        val userIdText = redisTemplate.opsForValue().get(tokenKey(refreshToken)) ?: return null
        val userId = userIdText.toLongOrNull() ?: return null
        val currentUserToken = redisTemplate.opsForValue().get(userKey(userId))

        return if (currentUserToken == refreshToken) userId else null
    }

    fun deleteByUserId(userId: Long) {
        val savedRefreshToken = redisTemplate.opsForValue().get(userKey(userId))
        if (!savedRefreshToken.isNullOrBlank()) {
            redisTemplate.delete(tokenKey(savedRefreshToken))
        }
        redisTemplate.delete(userKey(userId))
        log.info("[TokenService] 리프레시 토큰 삭제 완료: userId={}", userId)
    }

    fun extractRefreshToken(request: HttpServletRequest): String? {
        val cookies = request.cookies ?: return null
        return cookies.firstOrNull { it.name == refreshCookieName }?.value
    }

    fun addRefreshTokenCookie(response: HttpServletResponse, refreshToken: String) {
        val cookie = ResponseCookie.from(refreshCookieName, refreshToken)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path(refreshCookiePath)
            .maxAge(Duration.ofDays(refreshExpirationDays))
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
    }

    fun clearRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = ResponseCookie.from(refreshCookieName, "")
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path(refreshCookiePath)
            .maxAge(0)
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
    }

    private fun saveRefreshToken(userId: Long, refreshToken: String) {
        val ttl = Duration.ofDays(refreshExpirationDays)
        val oldRefreshToken = redisTemplate.opsForValue().get(userKey(userId))
        if (!oldRefreshToken.isNullOrBlank() && oldRefreshToken != refreshToken) {
            redisTemplate.delete(tokenKey(oldRefreshToken))
        }
        redisTemplate.opsForValue().set(userKey(userId), refreshToken, ttl)
        redisTemplate.opsForValue().set(tokenKey(refreshToken), userId.toString(), ttl)
    }

    private fun deleteRefreshToken(refreshToken: String) {
        val userId = redisTemplate.opsForValue().get(tokenKey(refreshToken))
        if (!userId.isNullOrBlank()) {
            redisTemplate.delete(userKey(userId.toLong()))
        }
        redisTemplate.delete(tokenKey(refreshToken))
    }

    private fun userKey(userId: Long): String = "refresh:user:$userId"
    private fun tokenKey(refreshToken: String): String = "refresh:token:$refreshToken"
}
