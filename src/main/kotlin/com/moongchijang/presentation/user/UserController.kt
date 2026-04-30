package com.moongchijang.presentation.user

import com.moongchijang.application.user.UserService
import com.moongchijang.application.user.dto.request.AdditionalInfoUpsertRequest
import com.moongchijang.application.user.dto.response.AdditionalInfoUpdatedResponse
import com.moongchijang.application.user.dto.response.NicknameAvailabilityResponse
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.principal.CustomUserPrincipal
import jakarta.validation.Valid
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
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/nickname/availability")
    fun checkNicknameAvailability(
        @RequestParam nickname: String,
    ): ApiResponse<NicknameAvailabilityResponse> {
        val duplicated = userService.existsByNickname(nickname)
        return ApiResponse.success(
            NicknameAvailabilityResponse(
                nickname = nickname,
                available = !duplicated,
            ),
        )
    }

    @PatchMapping("/me/additional-info")
    fun updateAdditionalInfo(
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
        @Valid @RequestBody request: AdditionalInfoUpsertRequest,
    ): ApiResponse<AdditionalInfoUpdatedResponse> {
        val userId = principal?.id ?: throw CustomException(ErrorCode.INVALID_LOGIN)
        val updatedUser = userService.updateAdditionalInfo(
            userId = userId,
            nickname = request.nickname,
            phoneNumber = request.phoneNumber,
        )
        return ApiResponse.success(AdditionalInfoUpdatedResponse.from(updatedUser))
    }
}