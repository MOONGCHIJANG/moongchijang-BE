package com.moongchijang.domain.user.presentation

import com.moongchijang.domain.user.application.UserService
import com.moongchijang.domain.user.application.OwnerWithdrawService
import com.moongchijang.domain.user.application.BusinessRegistrationLookupService
import com.moongchijang.domain.user.application.WithdrawalContextService
import com.moongchijang.domain.auth.application.TokenService
import com.moongchijang.domain.auth.application.dto.AuthUserResponse
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpsertRequest
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpdatedResponse
import com.moongchijang.domain.user.application.dto.BusinessRegistrationLookupRequest
import com.moongchijang.domain.user.application.dto.BusinessRegistrationLookupResponse
import com.moongchijang.domain.user.application.dto.MyPageRoleSwitchRequest
import com.moongchijang.domain.user.application.dto.NicknameAvailabilityResponse
import com.moongchijang.domain.user.application.dto.NicknameUpdateRequest
import com.moongchijang.domain.user.application.dto.NicknameUpdateResponse
import com.moongchijang.domain.user.application.dto.PasswordChangeRequest
import com.moongchijang.domain.user.application.dto.PasswordChangeResponse
import com.moongchijang.domain.user.application.dto.PhoneNumberUpdateRequest
import com.moongchijang.domain.user.application.dto.PhoneNumberUpdateResponse
import com.moongchijang.domain.user.application.dto.SellerBusinessProfileResponse
import com.moongchijang.domain.user.application.dto.SellerBusinessInfoUpsertRequest
import com.moongchijang.domain.user.application.dto.SellerSettlementAccountResponse
import com.moongchijang.domain.user.application.dto.SellerSettlementInfoUpsertRequest
import com.moongchijang.domain.user.application.dto.SellerSignupStatusResponse
import com.moongchijang.domain.user.application.dto.OwnerWithdrawRequest
import com.moongchijang.domain.user.application.dto.WithdrawRequest
import com.moongchijang.domain.user.application.dto.WithdrawalContextResponse
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.response.ApiResponse
import com.moongchijang.security.authorization.RequireCurrentRole
import com.moongchijang.security.principal.CustomUserPrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
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
    private val ownerWithdrawService: OwnerWithdrawService,
    private val withdrawalContextService: WithdrawalContextService,
    private val businessRegistrationLookupService: BusinessRegistrationLookupService,
    private val tokenService: TokenService,
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
        @AuthenticationPrincipal principal: CustomUserPrincipal?,
    ): ApiResponse<NicknameAvailabilityResponse> {
        val response = userService.checkNicknameAvailability(nickname, principal?.id)
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

    @PatchMapping("/me/role")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "마이페이지 역할 전환", description = "소비자/사장님 모드를 전환합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "전환 성공"),
            SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 모드 전환 권한 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun switchMyPageRole(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: MyPageRoleSwitchRequest,
    ): ApiResponse<AuthUserResponse> {
        log.info("[UserController] 마이페이지 역할 전환 요청 수신: userId={}, targetRole={}", principal.id, request.role)
        val response = userService.switchMyPageRole(principal.id, request.role)
        log.info("[UserController] 마이페이지 역할 전환 응답 완료: userId={}, targetRole={}", principal.id, request.role)
        return ApiResponse.success(response)
    }

    @GetMapping("/me/withdrawal-context")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "탈퇴 진입 컨텍스트 조회", description = "소비자/사장님 탈퇴 가능 상태와 권장 진입 화면을 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "사용자 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun getWithdrawalContext(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
    ): ApiResponse<WithdrawalContextResponse> {
        log.info("[UserController] 탈퇴 컨텍스트 조회 요청 수신: userId={}", principal.id)
        val response = withdrawalContextService.getContext(principal.id)
        log.info("[UserController] 탈퇴 컨텍스트 조회 응답 완료: userId={}", principal.id)
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

    @PatchMapping("/me/nickname")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "닉네임 변경", description = "인증된 사용자의 닉네임을 변경합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "변경 성공"),
            SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "409", description = "닉네임 중복", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun updateNickname(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: NicknameUpdateRequest,
    ): ApiResponse<NicknameUpdateResponse> {
        val response = userService.updateNickname(request, principal.id)
        return ApiResponse.success(response)
    }

    @PatchMapping("/me/phone-number")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "전화번호 변경", description = "인증된 사용자의 전화번호를 변경합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "변경 성공"),
            SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun updatePhoneNumber(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: PhoneNumberUpdateRequest,
    ): ApiResponse<PhoneNumberUpdateResponse> {
        val response = userService.updatePhoneNumber(request, principal.id)
        return ApiResponse.success(response)
    }

    @PatchMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "비밀번호 변경", description = "이메일 로그인 사용자의 비밀번호를 변경하고 세션을 무효화합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "변경 성공"),
            SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "이메일 사용자만 변경 가능", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun changePassword(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: PasswordChangeRequest,
        response: HttpServletResponse,
    ): ApiResponse<PasswordChangeResponse> {
        val changed = userService.changePassword(request, principal.id)
        tokenService.clearRefreshTokenCookie(response)
        return ApiResponse.success(changed)
    }

    @PostMapping("/me/seller/business-registration/lookup")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "사업자등록번호 조회", description = "사업자등록번호를 조회해 사업자 정보를 반환합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun lookupBusinessRegistration(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: BusinessRegistrationLookupRequest,
    ): ApiResponse<BusinessRegistrationLookupResponse> {
        log.info("[UserController] 사업자등록번호 조회 요청 수신: userId={}", principal.id)
        val response = businessRegistrationLookupService.lookup(request)
        log.info("[UserController] 사업자등록번호 조회 응답 완료: userId={}", principal.id)
        return ApiResponse.success(response)
    }

    @PatchMapping("/me/seller/business-info")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "사장님 사업자 정보 저장", description = "사장님 가입의 사업자 정보를 저장합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "저장 성공"),
            SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun upsertSellerBusinessInfo(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: SellerBusinessInfoUpsertRequest,
    ): ApiResponse<SellerSignupStatusResponse> {
        log.info("[UserController] 사장님 사업자 정보 저장 요청 수신: userId={}", principal.id)
        val response = userService.upsertSellerBusinessInfo(request, principal.id)
        log.info("[UserController] 사장님 사업자 정보 저장 응답 완료: userId={}", principal.id)
        return ApiResponse.success(response)
    }

    @PatchMapping("/me/seller/settlement-info")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "사장님 정산 정보 저장", description = "사장님 가입의 정산 정보를 저장하고 사장님 가입 완료 처리합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "저장 성공"),
            SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun upsertSellerSettlementInfo(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: SellerSettlementInfoUpsertRequest,
    ): ApiResponse<SellerSignupStatusResponse> {
        log.info("[UserController] 사장님 정산 정보 저장 요청 수신: userId={}", principal.id)
        val response = userService.upsertSellerSettlementInfo(request, principal.id)
        log.info("[UserController] 사장님 정산 정보 저장 응답 완료: userId={}", principal.id)
        return ApiResponse.success(response)
    }

    @GetMapping("/me/seller/settlement-account")
    @PreAuthorize("isAuthenticated()")
    @RequireCurrentRole(UserRole.SELLER)
    @Operation(summary = "사장님 입금 계좌 조회", description = "사장님의 현재 입금 계좌 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "계좌 정보 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun getSellerSettlementAccount(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
    ): ApiResponse<SellerSettlementAccountResponse> {
        log.info("[UserController] 사장님 입금 계좌 조회 요청 수신: userId={}", principal.id)
        val response = userService.getSellerSettlementAccount(principal.id)
        log.info("[UserController] 사장님 입금 계좌 조회 응답 완료: userId={}", principal.id)
        return ApiResponse.success(response)
    }

    @PatchMapping("/me/seller/settlement-account")
    @PreAuthorize("isAuthenticated()")
    @RequireCurrentRole(UserRole.SELLER)
    @Operation(summary = "사장님 입금 계좌 변경", description = "사장님의 입금 계좌 정보를 변경합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "변경 성공"),
            SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "계좌 정보 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun updateSellerSettlementAccount(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: SellerSettlementInfoUpsertRequest,
    ): ApiResponse<SellerSettlementAccountResponse> {
        log.info("[UserController] 사장님 입금 계좌 변경 요청 수신: userId={}", principal.id)
        val response = userService.updateSellerSettlementAccount(request, principal.id)
        log.info("[UserController] 사장님 입금 계좌 변경 응답 완료: userId={}", principal.id)
        return ApiResponse.success(response)
    }

    @GetMapping("/me/seller/business-profile")
    @PreAuthorize("isAuthenticated()")
    @RequireCurrentRole(UserRole.SELLER)
    @Operation(summary = "사장님 사업자 정보 조회", description = "사장님의 현재 사업자 정보를 조회합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "사업자 정보 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun getSellerBusinessProfile(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
    ): ApiResponse<SellerBusinessProfileResponse> {
        log.info("[UserController] 사장님 사업자 정보 조회 요청 수신: userId={}", principal.id)
        val response = userService.getSellerBusinessProfile(principal.id)
        log.info("[UserController] 사장님 사업자 정보 조회 응답 완료: userId={}", principal.id)
        return ApiResponse.success(response)
    }

    @PatchMapping("/me/seller/business-profile")
    @PreAuthorize("isAuthenticated()")
    @RequireCurrentRole(UserRole.SELLER)
    @Operation(summary = "사장님 사업자 정보 변경", description = "사장님의 사업자 정보를 변경합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "변경 성공"),
            SwaggerApiResponse(responseCode = "400", description = "입력값 검증 실패", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "404", description = "사업자 정보 없음", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun updateSellerBusinessProfile(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: SellerBusinessInfoUpsertRequest,
    ): ApiResponse<SellerBusinessProfileResponse> {
        log.info("[UserController] 사장님 사업자 정보 변경 요청 수신: userId={}", principal.id)
        val response = userService.updateSellerBusinessProfile(request, principal.id)
        log.info("[UserController] 사장님 사업자 정보 변경 응답 완료: userId={}", principal.id)
        return ApiResponse.success(response)
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @RequireCurrentRole(UserRole.BUYER)
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

    @DeleteMapping("/me/seller")
    @PreAuthorize("isAuthenticated()")
    @RequireCurrentRole(UserRole.SELLER)
    @Operation(summary = "사장님 회원 탈퇴", description = "사장님 회원 탈퇴를 처리합니다.")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "탈퇴 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "403", description = "사장님 권한 필요", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
            SwaggerApiResponse(responseCode = "409", description = "탈퇴 불가", content = [Content(schema = Schema(implementation = ApiResponse::class))]),
        ],
    )
    fun ownerWithdraw(
        @AuthenticationPrincipal principal: CustomUserPrincipal,
        @Valid @RequestBody request: OwnerWithdrawRequest,
    ): ApiResponse<Nothing> {
        log.info("[UserController] 사장님 회원탈퇴 요청 수신: userId={}", principal.id)
        ownerWithdrawService.withdraw(principal.id, request)
        log.info("[UserController] 사장님 회원탈퇴 응답 완료: userId={}", principal.id)
        return ApiResponse.success()
    }
}
