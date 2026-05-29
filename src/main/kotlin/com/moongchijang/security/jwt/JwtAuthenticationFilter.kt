package com.moongchijang.security.jwt

import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.security.principal.CustomUserPrincipal
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.slf4j.LoggerFactory

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    private val whitelist = listOf(
        "/health",
        "/swagger-ui",
        "/v3/api-docs",
        "/api/v1/auth/kakao",
        "/api/v1/auth/refresh",
        "/api/v1/auth/email",
        "/api/v1/auth/phone/verification-codes",
        "/api/v1/auth/phone/verification-codes/verify",
        "/api/v1/payments/portone/webhook",
        "/openapi.yaml",
        "/dev/openapi.yaml",
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
        val path = request.servletPath
        val method = request.method

        if (!token.isNullOrBlank()) {
            when (val tokenStatus = jwtTokenProvider.validateToken(token)) {
                TokenStatus.VALID -> {
                    val userId = jwtTokenProvider.getUserIdFromToken(token)
                    val user = userRepository.findById(userId).orElse(null)

                    if (user != null && user.deletedAt == null) {
                        val principal = CustomUserPrincipal(
                            id = user.id!!,
                            email = user.email,
                            role = user.role,
                        )

                        val authentication = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
                        SecurityContextHolder.getContext().authentication = authentication
                    } else {
                        log.warn(
                            "[JwtAuthenticationFilter] 유효 토큰 사용자 조회 실패 또는 탈퇴 사용자: method={}, path={}, userId={}",
                            method,
                            path,
                            userId,
                        )
                    }
                }
                TokenStatus.EXPIRED -> {
                    log.info(
                        "[JwtAuthenticationFilter] 만료된 액세스 토큰 감지: method={}, path={}, tokenStatus={}",
                        method,
                        path,
                        tokenStatus,
                    )
                    request.setAttribute(AUTH_EXCEPTION_ATTRIBUTE, ErrorCode.TOKEN_EXPIRED)
                }
                TokenStatus.INVALID -> {
                    log.info(
                        "[JwtAuthenticationFilter] 유효하지 않은 액세스 토큰 감지: method={}, path={}, tokenStatus={}",
                        method,
                        path,
                        tokenStatus,
                    )
                    request.setAttribute(AUTH_EXCEPTION_ATTRIBUTE, ErrorCode.TOKEN_INVALID)
                }
            }
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
        const val AUTH_EXCEPTION_ATTRIBUTE = "auth_exception"
    }
}
