package com.moongchijang.global.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class TraceIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(TRACE_ID_HEADER)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()

        MDC.put(TRACE_ID_MDC_KEY, traceId)
        request.setAttribute(TRACE_ID_REQUEST_ATTRIBUTE, traceId)
        response.setHeader(TRACE_ID_HEADER, traceId)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY)
        }
    }

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_ID_MDC_KEY = "traceId"
        const val TRACE_ID_REQUEST_ATTRIBUTE = "traceId"
    }
}
