package com.moongchijang.domain.auth.application

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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
) {
    private val refreshCookieName = "refreshToken"

    fun issueRefreshToken(userId: Long): String {
        val refreshToken = UUID.randomUUID().toString()
        saveRefreshToken(userId, refreshToken)
        return refreshToken
    }

    fun reissueRefreshToken(userId: Long, oldRefreshToken: String?): String {
        if (!oldRefreshToken.isNullOrBlank()) {
            deleteRefreshToken(oldRefreshToken)
        }
        val newRefreshToken = UUID.randomUUID().toString()
        saveRefreshToken(userId, newRefreshToken)
        return newRefreshToken
    }

    fun getUserIdByRefreshToken(refreshToken: String): Long? {
        val userId = redisTemplate.opsForValue().get(tokenKey(refreshToken)) ?: return null
        return userId.toLongOrNull()
    }

    fun deleteByUserId(userId: Long) {
        val savedRefreshToken = redisTemplate.opsForValue().get(userKey(userId))
        if (!savedRefreshToken.isNullOrBlank()) {
            redisTemplate.delete(tokenKey(savedRefreshToken))
        }
        redisTemplate.delete(userKey(userId))
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
            .path("/")
            .maxAge(Duration.ofDays(refreshExpirationDays))
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
    }

    fun clearRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = ResponseCookie.from(refreshCookieName, "")
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/")
            .maxAge(0)
            .build()

        response.addHeader("Set-Cookie", cookie.toString())
    }

    private fun saveRefreshToken(userId: Long, refreshToken: String) {
        val ttl = Duration.ofDays(refreshExpirationDays)
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
