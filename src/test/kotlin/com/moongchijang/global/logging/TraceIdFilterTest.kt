package com.moongchijang.global.logging

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class TraceIdFilterTest {

    private val filter = TraceIdFilter()

    @Test
    fun `요청 헤더의 traceId를 그대로 사용한다`() {
        val request = MockHttpServletRequest().apply {
            addHeader(TraceIdFilter.TRACE_ID_HEADER, "trace-id-from-client")
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { req, res ->
            assertEquals("trace-id-from-client", MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY))
            assertEquals("trace-id-from-client", req.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTRIBUTE))
            assertEquals("trace-id-from-client", (res as MockHttpServletResponse).getHeader(TraceIdFilter.TRACE_ID_HEADER))
        })

        assertNull(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY))
    }

    @Test
    fun `traceId 헤더가 없으면 새로 생성한다`() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { req, res ->
            val traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)
            assertNotNull(traceId)
            assertEquals(traceId, req.getAttribute(TraceIdFilter.TRACE_ID_REQUEST_ATTRIBUTE))
            assertEquals(traceId, (res as MockHttpServletResponse).getHeader(TraceIdFilter.TRACE_ID_HEADER))
        })

        assertNull(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY))
    }
}
