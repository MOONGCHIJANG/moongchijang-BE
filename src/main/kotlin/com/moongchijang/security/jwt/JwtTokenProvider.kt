package com.moongchijang.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") jwtSecret: String,
    @Value("\${jwt.access-token.expiration-minutes}") private val accessTokenExpirationMinutes: Long,
) {
    private val secretKey: SecretKey =
        Keys.hmacShaKeyFor(jwtSecret.toByteArray(StandardCharsets.UTF_8))

    fun generateAccessToken(userId: Long): String {
        val now = Date()
        val expiry = Date(now.time + TimeUnit.MINUTES.toMillis(accessTokenExpirationMinutes))

        return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiry)
            .signWith(secretKey)
            .compact()
    }

    fun validateToken(token: String): TokenStatus =
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)
            TokenStatus.VALID
        } catch (e: ExpiredJwtException) {
            TokenStatus.EXPIRED
        } catch (e: JwtException) {
            TokenStatus.INVALID
        }

    fun getUserIdFromToken(token: String): Long {
        val claims: Claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return claims.subject.toLong()
    }

    fun getRemainingMillis(token: String): Long {
        val claims: Claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
        return (claims.expiration.time - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun getAccessTokenExpiresInSeconds(): Long =
        TimeUnit.MINUTES.toSeconds(accessTokenExpirationMinutes)
}
