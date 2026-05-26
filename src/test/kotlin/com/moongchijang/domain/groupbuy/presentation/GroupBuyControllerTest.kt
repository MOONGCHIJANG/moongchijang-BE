package com.moongchijang.domain.groupbuy.presentation

import com.moongchijang.domain.groupbuy.application.GroupBuyService
import com.moongchijang.domain.groupbuy.application.GroupBuyViewerService
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyViewerCountResponse
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.security.principal.CustomUserPrincipal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import jakarta.servlet.http.Cookie

class GroupBuyControllerTest {

    private val groupBuyService: GroupBuyService = Mockito.mock(GroupBuyService::class.java)
    private val groupBuyViewerService: GroupBuyViewerService = Mockito.mock(GroupBuyViewerService::class.java)
    private val controller = GroupBuyController(groupBuyService, groupBuyViewerService)

    @Test
    fun `비로그인 조회자 heartbeat 요청 시 쿠키가 없으면 viewerSessionId를 발급한다`() {
        Mockito.`when`(groupBuyViewerService.heartbeat(Mockito.eq(101L), Mockito.isNull(), Mockito.anyString()))
            .thenReturn(GroupBuyViewerCountResponse(1, false, 10))
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        controller.heartbeatViewer(
            groupBuyId = 101L,
            principal = null,
            request = request,
            response = response,
        )

        val setCookie = response.getHeader("Set-Cookie")
        assertNotNull(setCookie)
        val cookieHeader = setCookie!!
        assertTrue(cookieHeader.contains("viewerSessionId="))
        assertTrue(cookieHeader.contains("Max-Age=2592000"))
        assertTrue(cookieHeader.contains("HttpOnly"))
        assertTrue(cookieHeader.contains("SameSite=Lax"))
    }

    @Test
    fun `비로그인 조회자 heartbeat 요청 시 기존 쿠키를 재사용한다`() {
        Mockito.`when`(groupBuyViewerService.heartbeat(102L, null, "session-fixed-123"))
            .thenReturn(GroupBuyViewerCountResponse(2, false, 10))
        val request = MockHttpServletRequest()
        request.setCookies(Cookie("viewerSessionId", "session-fixed-123"))
        val response = MockHttpServletResponse()

        controller.heartbeatViewer(
            groupBuyId = 102L,
            principal = null,
            request = request,
            response = response,
        )

        assertEquals(null, response.getHeader("Set-Cookie"))
        Mockito.verify(groupBuyViewerService)
            .heartbeat(102L, null, "session-fixed-123")
    }

    @Test
    fun `로그인 조회자 heartbeat 요청 시 userId를 우선 사용한다`() {
        Mockito.`when`(groupBuyViewerService.heartbeat(103L, 77L, "session-login-123"))
            .thenReturn(GroupBuyViewerCountResponse(3, false, 10))
        val request = MockHttpServletRequest()
        request.setCookies(Cookie("viewerSessionId", "session-login-123"))
        val response = MockHttpServletResponse()
        val principal = CustomUserPrincipal(
            id = 77L,
            email = "viewer@example.com",
            role = UserRole.BUYER,
        )
        controller.heartbeatViewer(
            groupBuyId = 103L,
            principal = principal,
            request = request,
            response = response,
        )

        Mockito.verify(groupBuyViewerService).heartbeat(103L, 77L, "session-login-123")
        assertEquals(null, response.getHeader("Set-Cookie"))
    }
}
