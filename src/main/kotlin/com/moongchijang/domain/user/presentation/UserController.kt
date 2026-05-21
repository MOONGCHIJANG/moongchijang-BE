package com.moongchijang.domain.user.presentation

import com.moongchijang.domain.user.application.UserService
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpsertRequest
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpdatedResponse
import com.moongchijang.domain.user.application.dto.NicknameAvailabilityResponse
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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 API")
class UserController(
    private val userService: UserService,
) {

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
}
