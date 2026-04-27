package com.moongchijang.security.jwt

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
) : OncePerRequestFilter() {

    private val whitelist = listOf(
        "/health",
        "/swagger-ui",
        "/v3/api-docs",
        "/api/v1/auth/kakao",
        "/api/v1/auth/refresh",
    )

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.servletPath
        return whitelist.any { path == it || path.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (!token.isNullOrBlank() && jwtTokenProvider.validateToken(token) == TokenStatus.VALID) {
            val userId = jwtTokenProvider.getUserIdFromToken(token)
            val authentication = UsernamePasswordAuthenticationToken(userId, null, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return null
        if (!bearer.startsWith("Bearer ")) return null
        return bearer.substring(TOKEN_PREFIX.length)
    }

    companion object {
        private const val TOKEN_PREFIX = "Bearer "
    }
}
