package com.moongchijang.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
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
    private val objectMapper = ObjectMapper()

    private val filter = JwtAuthenticationFilter(
        jwtTokenProvider = jwtTokenProvider,
        userRepository = userRepository,
        objectMapper = objectMapper,
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
    fun `만료 토큰이면 401 TOKEN_EXPIRED 반환하고 체인을 진행하지 않는다`() {
        val request = MockHttpServletRequest().apply {
            method = "GET"
            requestURI = "/api/v1/users/me"
            addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired-token")
        }
        val response = MockHttpServletResponse()
        val chain: FilterChain = mock(FilterChain::class.java)

        `when`(jwtTokenProvider.validateToken("expired-token")).thenReturn(TokenStatus.EXPIRED)

        filter.doFilter(request, response, chain)

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
        assertTrue(response.contentAsString.contains("\"code\":\"TOKEN_EXPIRED\""))
        verify(chain, never()).doFilter(request, response)
    }

    @Test
    fun `무효 토큰이면 401 TOKEN_INVALID 반환하고 체인을 진행하지 않는다`() {
        val request = MockHttpServletRequest().apply {
            method = "GET"
            requestURI = "/api/v1/users/me"
            addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
        }
        val response = MockHttpServletResponse()
        val chain: FilterChain = mock(FilterChain::class.java)

        `when`(jwtTokenProvider.validateToken("invalid-token")).thenReturn(TokenStatus.INVALID)

        filter.doFilter(request, response, chain)

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
        assertTrue(response.contentAsString.contains("\"code\":\"TOKEN_INVALID\""))
        verify(chain, never()).doFilter(request, response)
    }
}
