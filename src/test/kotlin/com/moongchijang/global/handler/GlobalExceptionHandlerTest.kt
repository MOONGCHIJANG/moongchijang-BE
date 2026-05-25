package com.moongchijang.global.handler

import com.moongchijang.domain.mypage.application.dto.MypageParticipationStatusFilter
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `필수 status 쿼리 파라미터가 누락되면 400으로 응답한다`() {
        val exception = MissingServletRequestParameterException("status", "MypageParticipationStatusFilter")

        val response = handler.handleMissingServletRequestParameterException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertFalse(response.body!!.success)
        assertEquals(ErrorCode.INVALID_INPUT.name, response.body!!.error?.code)
        assertEquals("status: 필수 요청 파라미터가 누락되었습니다", response.body!!.error?.detail)
    }

    @Test
    fun `status enum 변환에 실패하면 400으로 응답한다`() {
        val exception = MethodArgumentTypeMismatchException(
            "INVALID",
            MypageParticipationStatusFilter::class.java,
            "status",
            statusMethodParameter(),
            IllegalArgumentException("No enum constant")
        )

        val response = handler.handleMethodArgumentTypeMismatchException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertFalse(response.body!!.success)
        assertEquals(ErrorCode.INVALID_INPUT.name, response.body!!.error?.code)
        assertEquals("status: 올바른 형식이 아닙니다", response.body!!.error?.detail)
    }

    @Suppress("unused")
    private fun statusParameter(status: MypageParticipationStatusFilter) = status

    private fun statusMethodParameter(): MethodParameter {
        val method = this::class.java.getDeclaredMethod("statusParameter", MypageParticipationStatusFilter::class.java)
        return MethodParameter(method, 0)
    }
}
