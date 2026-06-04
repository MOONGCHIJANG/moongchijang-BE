package com.moongchijang.security.jwt

import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.security.crypto.AesGcmPersonalInfoEncryptor
import com.moongchijang.security.crypto.HmacSha256PersonalInfoHasher
import com.moongchijang.security.crypto.PersonalInfoEncryptionProperties
import com.moongchijang.security.crypto.PersonalInfoManager
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

class JwtAuthenticationFilterTest {

    private val jwtTokenProvider: JwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val personalInfoProperties = PersonalInfoEncryptionProperties(
        secretKey = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    )
    private val personalInfoManager = PersonalInfoManager(
        AesGcmPersonalInfoEncryptor(personalInfoProperties),
        HmacSha256PersonalInfoHasher(personalInfoProperties),
    )

    private val filter = JwtAuthenticationFilter(
        jwtTokenProvider = jwtTokenProvider,
        userRepository = userRepository,
        personalInfoManager = personalInfoManager,
    )

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `유효 토큰이면 인증 컨텍스트를 세팅하고 다음 필터로 진행한다`() {
        val request = MockHttpServletRequest().apply {
            method = "GET"
            requestURI = "/api/v1/users/me"
            addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
        }
        val response = MockHttpServletResponse()
        val chain: FilterChain = mock(FilterChain::class.java)
        val user = User(
            provider = AuthProvider.KAKAO,
            providerId = "kakao-1",
            email = "test@example.com",
            id = 7L,
        )

        `when`(jwtTokenProvider.validateToken("valid-token")).thenReturn(TokenStatus.VALID)
        `when`(jwtTokenProvider.getUserIdFromToken("valid-token")).thenReturn(7L)
        `when`(userRepository.findById(7L)).thenReturn(Optional.of(user))

        filter.doFilter(request, response, chain)

        assertNotNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `만료 토큰이면 요청 attribute에 TOKEN_EXPIRED를 기록하고 체인을 진행한다`() {
        val request = MockHttpServletRequest().apply {
            method = "GET"
            requestURI = "/api/v1/users/me"
            addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired-token")
        }
        val response = MockHttpServletResponse()
        val chain: FilterChain = mock(FilterChain::class.java)

        `when`(jwtTokenProvider.validateToken("expired-token")).thenReturn(TokenStatus.EXPIRED)

        filter.doFilter(request, response, chain)

        assertEquals(ErrorCode.TOKEN_EXPIRED, request.getAttribute(JwtAuthenticationFilter.AUTH_EXCEPTION_ATTRIBUTE))
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }

    @Test
    fun `무효 토큰이면 요청 attribute에 TOKEN_INVALID를 기록하고 체인을 진행한다`() {
        val request = MockHttpServletRequest().apply {
            method = "GET"
            requestURI = "/api/v1/users/me"
            addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
        }
        val response = MockHttpServletResponse()
        val chain: FilterChain = mock(FilterChain::class.java)

        `when`(jwtTokenProvider.validateToken("invalid-token")).thenReturn(TokenStatus.INVALID)

        filter.doFilter(request, response, chain)

        assertEquals(ErrorCode.TOKEN_INVALID, request.getAttribute(JwtAuthenticationFilter.AUTH_EXCEPTION_ATTRIBUTE))
        assertNull(SecurityContextHolder.getContext().authentication)
        verify(chain).doFilter(request, response)
    }
}
