package com.moongchijang.domain.auth.presentation

import com.moongchijang.domain.auth.application.PhoneVerificationService
import com.moongchijang.domain.auth.application.dto.PhoneVerificationCodeSendRequest
import com.moongchijang.domain.auth.application.dto.PhoneVerificationCodeSentResponse
import com.moongchijang.domain.auth.application.dto.PhoneVerificationCodeVerifyRequest
import com.moongchijang.domain.auth.application.dto.PhoneVerificationVerifiedResponse
import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/phone/verification-codes")
@Tag(name = "Auth", description = "인증 API")
class PhoneVerificationController(
    private val phoneVerificationService: PhoneVerificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    @Operation(summary = "전화번호 인증코드 발송", description = "입력한 전화번호로 6자리 인증코드를 발송합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "인증코드 발송 성공"),
            SwaggerApiResponse(responseCode = "400", description = "잘못된 요청", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "500", description = "문자 발송 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun sendVerificationCode(
        @Valid @RequestBody request: PhoneVerificationCodeSendRequest,
    ): ApiResponse<PhoneVerificationCodeSentResponse> {
        log.info("[PhoneVerificationController] 전화번호 인증코드 발송 요청 수신")
        val response = phoneVerificationService.sendVerificationCode(request)
        log.info("[PhoneVerificationController] 전화번호 인증코드 발송 응답 완료")
        return ApiResponse.success(response)
    }

    @PostMapping("/verify")
    @Operation(summary = "전화번호 인증코드 확인", description = "전화번호와 인증코드를 검증해 인증 완료 상태를 처리합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "인증코드 확인 성공"),
            SwaggerApiResponse(responseCode = "400", description = "인증코드 불일치/만료", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun verifyCode(
        @Valid @RequestBody request: PhoneVerificationCodeVerifyRequest,
    ): ApiResponse<PhoneVerificationVerifiedResponse> {
        log.info("[PhoneVerificationController] 전화번호 인증코드 검증 요청 수신")
        val response = phoneVerificationService.verifyCode(request)
        log.info("[PhoneVerificationController] 전화번호 인증코드 검증 응답 완료")
        return ApiResponse.success(response)
    }
}
