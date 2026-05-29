package com.moongchijang.global.config

import com.moongchijang.security.principal.CustomUserPrincipal
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class ApiRequestLoggingInterceptor : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(ApiRequestLoggingInterceptor::class.java)

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        request.setAttribute(REQUEST_START_TIME_ATTR, System.currentTimeMillis())
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        val startedAt = request.getAttribute(REQUEST_START_TIME_ATTR) as? Long ?: System.currentTimeMillis()
        val elapsedMs = System.currentTimeMillis() - startedAt
        val userId = (request.userPrincipal as? CustomUserPrincipal)?.id

        log.info(
            "[ApiRequestLoggingInterceptor] method={}, path={}, status={}, elapsedMs={}, userId={}",
            request.method,
            request.requestURI,
            response.status,
            elapsedMs,
            userId,
        )
    }

    companion object {
        private const val REQUEST_START_TIME_ATTR = "api.request.startTime"
    }
}
