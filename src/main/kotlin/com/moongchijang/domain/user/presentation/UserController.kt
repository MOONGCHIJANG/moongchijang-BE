package com.moongchijang.domain.user.presentation

import com.moongchijang.domain.user.application.UserService
import com.moongchijang.domain.auth.application.dto.AuthUserResponse
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpsertRequest
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpdatedResponse
import com.moongchijang.domain.user.application.dto.NicknameAvailabilityResponse
import com.moongchijang.domain.user.application.dto.WithdrawRequest
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.slf4j.LoggerFactory

@Validated
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 API")
class UserController(
    private val userService: UserService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/nickname/availability")
    @Operation(summary = "닉네임 중복 확인", description = "입력한 닉네임의 사용 가능 여부를 반환합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "400", description = "닉네임 형식 오류", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun checkNicknameAvailability(
        @RequestParam nickname: String,
    ): ApiResponse<NicknameAvailabilityResponse> {
        val response = userService.checkNicknameAvailability(nickname)
        return ApiResponse.success(response)
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "내 정보 조회", description = "인증된 사용자의 계정 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "사용자 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun getMyInfo(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
    ): ApiResponse<AuthUserResponse> {
        log.info("[UserController] 내 정보 조회 요청 수신: userId={}", principal.id)
        val response = userService.getMyInfo(principal.id)
        log.info("[UserController] 내 정보 조회 응답 완료: userId={}", principal.id)
        return ApiResponse.success(response)
    }

    @PatchMapping("/me/additional-info")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "추가정보 입력", description = "신규 가입자의 닉네임/전화번호를 저장하고 가입 완료 상태를 갱신합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "저장 성공"),
            SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "409", description = "닉네임 중복", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun updateAdditionalInfo(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: AdditionalInfoUpsertRequest,
    ): ApiResponse<AdditionalInfoUpdatedResponse> {
        val response = userService.updateAdditionalInfo(request, principal.id)
        return ApiResponse.success(response)
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 처리합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "탈퇴 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "409", description = "탈퇴 불가", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun withdraw(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: WithdrawRequest,
    ): ApiResponse<Nothing> {
        log.info("[UserController] 회원탈퇴 요청 수신: userId={}", principal.id)
        userService.withdraw(principal.id, request)
        log.info("[UserController] 회원탈퇴 응답 완료: userId={}", principal.id)
        return ApiResponse.success()
    }
}
