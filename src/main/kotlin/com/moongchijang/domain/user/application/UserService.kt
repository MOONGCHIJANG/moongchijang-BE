package com.moongchijang.domain.user.application

import com.moongchijang.domain.auth.application.PhoneVerificationService
import com.moongchijang.domain.auth.application.TokenService
import com.moongchijang.domain.notification.application.discord.AdminDiscordAlertService
import com.moongchijang.domain.auth.application.dto.AuthUserResponse
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.payment.application.PaymentService
import com.moongchijang.domain.payment.application.dto.CancelParticipationRequest
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpsertRequest
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpdatedResponse
import com.moongchijang.domain.user.application.dto.EmailAvailabilityResponse
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
import com.moongchijang.domain.user.application.dto.WithdrawRequest
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.SellerBusinessProfile
import com.moongchijang.domain.user.domain.entity.SellerSettlementAccount
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.entity.WithdrawalReason
import com.moongchijang.domain.user.domain.entity.WithdrawnAccount
import com.moongchijang.domain.user.domain.repository.SellerBusinessProfileRepository
import com.moongchijang.domain.user.domain.repository.SellerSettlementAccountRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.domain.user.domain.repository.WithdrawnAccountRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.MaskingUtils.maskEmail
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository,
    private val sellerBusinessProfileRepository: SellerBusinessProfileRepository,
    private val sellerSettlementAccountRepository: SellerSettlementAccountRepository,
    private val phoneVerificationService: PhoneVerificationService,
    private val tokenService: TokenService,
    private val participationRepository: ParticipationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val paymentService: PaymentService,
    private val passwordEncoder: PasswordEncoder,
    private val adminDiscordAlertService: AdminDiscordAlertService,
    private val withdrawnAccountRepository: WithdrawnAccountRepository,
    private val withdrawnAccountCommandService: WithdrawnAccountCommandService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun findOrCreateKakaoUser(
        providerId: String,
        email: String,
        nickname: String,
    ): Pair<User, Boolean> {
        findActiveKakaoUser(providerId)?.let {
            log.info("[UserService] 기존 카카오 사용자 로그인 처리: userId={}", it.id)
            return it to false
        }
        validateKakaoRejoinAvailable(providerId)
        log.info("[UserService] 신규 카카오 사용자 생성 처리")
        return createNewKakaoUser(providerId, email, nickname) to true
    }

    @Transactional
    fun createEmailUser(email: String, passwordHash: String): User {
        val normalizedEmail = normalizeEmail(email)
        log.info("[UserService] 이메일 사용자 생성 시작: email={}", maskEmail(normalizedEmail))
        validateEmailFormat(normalizedEmail)

        if (userRepository.existsByProviderAndEmailAndDeletedAtIsNull(AuthProvider.EMAIL, normalizedEmail)) {
            throw CustomException(ErrorCode.DUPLICATE_EMAIL)
        }
        validateEmailRejoinAvailable(normalizedEmail)

        val user = User.newEmailUser(
            email = normalizedEmail,
            passwordHash = passwordHash,
        )
        val savedUser = try {
            userRepository.save(user)
        } catch (e: DataIntegrityViolationException) {
            // provider+email 유니크 인덱스 충돌(동시성 가입 요청) 시 도메인 예외로 변환
            throw CustomException(ErrorCode.DUPLICATE_EMAIL)
        }
        log.info("[UserService] 이메일 사용자 생성 완료: userId={}", savedUser.id)

        return savedUser
    }

    @Transactional(readOnly = true)
    fun findActiveEmailUser(email: String): User? {
        val normalizedEmail = normalizeEmail(email)
        validateEmailFormat(normalizedEmail)

        return userRepository.findByProviderAndEmailAndDeletedAtIsNull(
            provider = AuthProvider.EMAIL,
            email = normalizedEmail,
        )
    }

    @Transactional(readOnly = true)
    fun checkNicknameAvailability(nickname: String, userId: Long?): NicknameAvailabilityResponse {
        validateNicknameFormat(nickname)
        val duplicated = isNicknameDuplicated(nickname, userId)
        return NicknameAvailabilityResponse(
            nickname = nickname,
            available = !duplicated,
        )
    }

    @Transactional(readOnly = true)
    fun getMyInfo(userId: Long): AuthUserResponse {
        log.info("[UserService] 내 정보 조회 시작: userId={}", userId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        log.info("[UserService] 내 정보 조회 완료: userId={}", userId)
        return AuthUserResponse.from(user)
    }

    @Transactional(readOnly = true)
    fun checkEmailAvailability(email: String): EmailAvailabilityResponse {
        val normalizedEmail = normalizeEmail(email)
        log.info("[UserService] 이메일 중복 확인 시작: email={}", maskEmail(normalizedEmail))
        validateEmailFormat(normalizedEmail)

        val duplicated = userRepository.existsByProviderAndEmailAndDeletedAtIsNull(AuthProvider.EMAIL, normalizedEmail) ||
            isEmailRejoinBlocked(normalizedEmail)
        val response = EmailAvailabilityResponse(
            email = normalizedEmail,
            available = !duplicated,
        )

        log.info(
            "[UserService] 이메일 중복 확인 완료: email={}, available={}",
            maskEmail(normalizedEmail),
            response.available,
        )
        return response
    }

    @Transactional
    fun updateAdditionalInfo(request: AdditionalInfoUpsertRequest, userId: Long): AdditionalInfoUpdatedResponse {
        log.info("[UserService] 추가정보 입력 처리 시작: userId={}", userId)
        validateNicknameFormat(request.nickname)
        validatePhoneNumberFormat(request.phoneNumber)

        phoneVerificationService.ensureVerified(request.phoneNumber)

        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        assertNoDuplicateNickname(request.nickname, userId)

        user.completeSignup(request.nickname, request.phoneNumber)
        log.info("[UserService] 추가정보 입력 처리 완료: userId={}", userId)
        return AdditionalInfoUpdatedResponse.from(user)
    }

    @Transactional
    fun saveLastRole(userId: Long, role: UserRole) {
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        user.saveLastRole(role)
    }

    @Transactional
    fun switchMyPageRole(userId: Long, targetRole: UserRole): AuthUserResponse {
        log.info("[UserService] 마이페이지 역할 전환 시작: userId={}, targetRole={}", userId, targetRole)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (!user.hasRole(targetRole)) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        user.role = targetRole
        user.saveLastRole(targetRole)
        log.info("[UserService] 마이페이지 역할 전환 완료: userId={}, targetRole={}", userId, targetRole)
        return AuthUserResponse.from(user)
    }

    @Transactional(readOnly = true)
    fun getSellerSettlementAccount(userId: Long): SellerSettlementAccountResponse {
        log.info("[UserService] 사장님 입금 계좌 조회 시작: userId={}", userId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        ensureSellerRole(user)

        val account = sellerSettlementAccountRepository.findByUserId(userId)
            ?: throw CustomException(ErrorCode.SELLER_BUSINESS_INFO_REQUIRED)
        log.info("[UserService] 사장님 입금 계좌 조회 완료: userId={}", userId)
        return SellerSettlementAccountResponse(
            bankCode = account.bankCode,
            accountNumber = account.accountNumber,
            accountHolderName = account.accountHolderName,
        )
    }

    @Transactional
    fun updateSellerSettlementAccount(
        request: SellerSettlementInfoUpsertRequest,
        userId: Long,
    ): SellerSettlementAccountResponse {
        log.info("[UserService] 사장님 입금 계좌 변경 시작: userId={}", userId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        ensureSellerRole(user)

        val account = sellerSettlementAccountRepository.findByUserId(userId)
            ?: throw CustomException(ErrorCode.SELLER_BUSINESS_INFO_REQUIRED)
        account.bankCode = SettlementInstitutionCodeMapper.toCode(request.bankCode)
        account.accountNumber = request.accountNumber.trim()
        account.accountHolderName = request.accountHolderName.trim()
        sellerSettlementAccountRepository.save(account)

        log.info("[UserService] 사장님 입금 계좌 변경 완료: userId={}", userId)
        return SellerSettlementAccountResponse(
            bankCode = account.bankCode,
            accountNumber = account.accountNumber,
            accountHolderName = account.accountHolderName,
        )
    }

    @Transactional(readOnly = true)
    fun getSellerBusinessProfile(userId: Long): SellerBusinessProfileResponse {
        log.info("[UserService] 사장님 사업자 정보 조회 시작: userId={}", userId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        ensureSellerRole(user)

        val profile = sellerBusinessProfileRepository.findByUserId(userId)
            ?: throw CustomException(ErrorCode.SELLER_BUSINESS_INFO_REQUIRED)
        log.info("[UserService] 사장님 사업자 정보 조회 완료: userId={}", userId)
        return SellerBusinessProfileResponse(
            businessRegistrationNumber = profile.businessRegistrationNumber,
            storeName = profile.storeName,
            ownerName = profile.ownerName,
            storeAddress = profile.storeAddress,
            phoneNumber = profile.phoneNumber,
        )
    }

    @Transactional
    fun updateSellerBusinessProfile(
        request: SellerBusinessInfoUpsertRequest,
        userId: Long,
    ): SellerBusinessProfileResponse {
        log.info("[UserService] 사장님 사업자 정보 변경 시작: userId={}", userId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        ensureSellerRole(user)

        val profile = sellerBusinessProfileRepository.findByUserId(userId)
            ?: throw CustomException(ErrorCode.SELLER_BUSINESS_INFO_REQUIRED)

        profile.businessRegistrationNumber = normalizeBusinessRegistrationNumber(request.businessRegistrationNumber)
        profile.storeName = request.storeName.trim()
        profile.ownerName = request.ownerName.trim()
        profile.storeAddress = request.storeAddress.trim()
        profile.phoneNumber = request.phoneNumber.trim()
        sellerBusinessProfileRepository.save(profile)

        log.info("[UserService] 사장님 사업자 정보 변경 완료: userId={}", userId)
        return SellerBusinessProfileResponse(
            businessRegistrationNumber = profile.businessRegistrationNumber,
            storeName = profile.storeName,
            ownerName = profile.ownerName,
            storeAddress = profile.storeAddress,
            phoneNumber = profile.phoneNumber,
        )
    }

    @Transactional
    fun updateNickname(request: NicknameUpdateRequest, userId: Long): NicknameUpdateResponse {
        log.info("[UserService] 닉네임 변경 처리 시작: userId={}", userId)
        validateNicknameFormat(request.nickname)

        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        assertNoDuplicateNickname(request.nickname, userId)

        user.nickname = request.nickname
        log.info("[UserService] 닉네임 변경 처리 완료: userId={}", userId)
        return NicknameUpdateResponse(
            id = userId,
            nickname = request.nickname,
        )
    }

    @Transactional
    fun updatePhoneNumber(request: PhoneNumberUpdateRequest, userId: Long): PhoneNumberUpdateResponse {
        log.info("[UserService] 전화번호 변경 처리 시작: userId={}", userId)
        validatePhoneNumberFormat(request.phoneNumber)
        phoneVerificationService.ensureVerifiedForUser(userId, request.phoneNumber)

        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        user.phoneNumber = request.phoneNumber
        log.info("[UserService] 전화번호 변경 처리 완료: userId={}", userId)
        return PhoneNumberUpdateResponse(
            id = userId,
            phoneNumber = request.phoneNumber,
        )
    }

    @Transactional
    fun changePassword(request: PasswordChangeRequest, userId: Long): PasswordChangeResponse {
        log.info("[UserService] 비밀번호 변경 처리 시작: userId={}", userId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (user.provider != AuthProvider.EMAIL) {
            throw CustomException(ErrorCode.EMAIL_PASSWORD_CHANGE_NOT_ALLOWED)
        }

        val currentPasswordHash = user.passwordHash ?: throw CustomException(ErrorCode.INVALID_CREDENTIALS)
        if (!passwordEncoder.matches(request.currentPassword, currentPasswordHash)) {
            throw CustomException(ErrorCode.PASSWORD_CHANGE_CURRENT_PASSWORD_MISMATCH)
        }

        validatePasswordPolicyForChange(
            email = user.email ?: throw CustomException(ErrorCode.INVALID_CREDENTIALS),
            newPassword = request.newPassword,
        )

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        tokenService.deleteByUserId(userId)
        log.info("[UserService] 비밀번호 변경 처리 완료: userId={}", userId)
        return PasswordChangeResponse(changed = true)
    }

    @Transactional
    fun upsertSellerBusinessInfo(request: SellerBusinessInfoUpsertRequest, userId: Long): SellerSignupStatusResponse {
        log.info("[UserService] 사장님 사업자 정보 저장 시작: userId={}", userId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        val normalizedBusinessRegistrationNumber = normalizeBusinessRegistrationNumber(request.businessRegistrationNumber)
        val profile = sellerBusinessProfileRepository.findByUserId(userId)?.apply {
            businessRegistrationNumber = normalizedBusinessRegistrationNumber
            storeName = request.storeName.trim()
            ownerName = request.ownerName.trim()
            storeAddress = request.storeAddress.trim()
            phoneNumber = request.phoneNumber.trim()
        } ?: SellerBusinessProfile(
            user = user,
            businessRegistrationNumber = normalizedBusinessRegistrationNumber,
            storeName = request.storeName.trim(),
            ownerName = request.ownerName.trim(),
            storeAddress = request.storeAddress.trim(),
            phoneNumber = request.phoneNumber.trim(),
        )
        sellerBusinessProfileRepository.save(profile)

        log.info("[UserService] 사장님 사업자 정보 저장 완료: userId={}", userId)
        return SellerSignupStatusResponse(
            id = userId,
            sellerSignupCompleted = user.sellerSignupCompleted,
        )
    }

    @Transactional
    fun upsertSellerSettlementInfo(request: SellerSettlementInfoUpsertRequest, userId: Long): SellerSignupStatusResponse {
        log.info("[UserService] 사장님 정산 정보 저장 시작: userId={}", userId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        if (!sellerBusinessProfileRepository.existsByUserId(userId)) {
            throw CustomException(ErrorCode.SELLER_BUSINESS_INFO_REQUIRED)
        }

        val normalizedInstitutionCode = SettlementInstitutionCodeMapper.toCode(request.bankCode)
        val account = sellerSettlementAccountRepository.findByUserId(userId)?.apply {
            bankCode = normalizedInstitutionCode
            accountNumber = request.accountNumber.trim()
            accountHolderName = request.accountHolderName.trim()
        } ?: SellerSettlementAccount(
            user = user,
            bankCode = normalizedInstitutionCode,
            accountNumber = request.accountNumber.trim(),
            accountHolderName = request.accountHolderName.trim(),
        )
        sellerSettlementAccountRepository.save(account)

        user.grantRole(UserRole.SELLER)
        user.role = UserRole.SELLER
        user.saveLastRole(UserRole.SELLER)
        user.completeSellerSignup()
        sellerBusinessProfileRepository.findByUserId(userId)?.let { profile ->
            adminDiscordAlertService.sendNewSellerSignup(profile)
        }

        log.info("[UserService] 사장님 정산 정보 저장 완료: userId={}", userId)
        return SellerSignupStatusResponse(
            id = userId,
            sellerSignupCompleted = user.sellerSignupCompleted,
        )
    }

    @Transactional
    fun withdraw(userId: Long, request: WithdrawRequest) {
        log.info("[UserService] 회원탈퇴 처리 시작: userId={}", userId)
        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        if (user.hasRole(UserRole.SELLER)) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }

        validateWithdrawalReason(request)
        validateWithdrawable(userId)
        cancelActiveParticipationsForWithdrawal(userId)
        val deletedFavoriteCount = favoriteRepository.deleteByUserId(userId)
        log.info("[UserService] 회원탈퇴 찜 삭제 완료: userId={}, deletedCount={}", userId, deletedFavoriteCount)

        user.withdraw(
            reason = request.reason,
            reasonDetail = normalizedReasonDetail(request),
        )
        userRepository.save(user)
        withdrawnAccountCommandService.recordWithdrawal(
            user = user,
            withdrawnAt = requireNotNull(user.deletedAt),
        )
        tokenService.deleteByUserId(userId)

        log.info("[UserService] 회원탈퇴 처리 완료: userId={}", userId)
    }


    fun validateWithdrawable(userId: Long) {
        val hasPendingPickup = participationRepository.existsPendingPickupForWithdrawal(
            userId = userId,
            participationStatus = ParticipationStatus.CONFIRMED,
            pickupStatuses = listOf(PickupStatus.NOT_READY, PickupStatus.READY),
            groupBuyStatuses = listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
        )

        if (hasPendingPickup) {
            throw CustomException(ErrorCode.WITHDRAWAL_BLOCKED_PENDING_PICKUP)
        }
    }

    private fun cancelActiveParticipationsForWithdrawal(userId: Long) {
        val activeParticipations = participationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
            userId = userId,
            status = ParticipationStatus.PAID_WAITING_GOAL,
        )

        if (activeParticipations.isEmpty()) {
            return
        }

        log.info("[UserService] 회원탈퇴 참여중 공구 자동 취소 시작: userId={}, targetCount={}", userId, activeParticipations.size)
        val request = CancelParticipationRequest(
            reason = ParticipationCancelReason.OTHER,
            reasonDetail = "회원탈퇴 자동 취소",
        )
        activeParticipations.forEach { participation ->
            paymentService.cancelParticipation(
                participationId = participation.id,
                userId = userId,
                request = request,
            )
        }
        log.info("[UserService] 회원탈퇴 참여중 공구 자동 취소 완료: userId={}, targetCount={}", userId, activeParticipations.size)
    }

    private fun validateWithdrawalReason(request: WithdrawRequest) {
        if (request.reason == WithdrawalReason.OTHER && request.reasonDetail.isNullOrBlank()) {
            throw CustomException(ErrorCode.WITHDRAWAL_REASON_DETAIL_REQUIRED)
        }
    }

    private fun normalizedReasonDetail(request: WithdrawRequest): String? {
        if (request.reason != WithdrawalReason.OTHER) {
            return null
        }
        return request.reasonDetail?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun normalizeBusinessRegistrationNumber(raw: String): String = raw.replace(Regex("[^0-9]"), "")

    private fun ensureSellerRole(user: User) {
        if (!user.hasRole(UserRole.SELLER)) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
    }

    private fun findActiveKakaoUser(providerId: String): User? {
        return userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(
            provider = AuthProvider.KAKAO,
            providerId = providerId,
        )
    }

    private fun findWithdrawnKakaoAccount(providerId: String): WithdrawnAccount? {
        return withdrawnAccountRepository.findByProviderAndProviderId(
            provider = AuthProvider.KAKAO,
            providerId = providerId,
        )
    }

    private fun findWithdrawnEmailAccount(email: String): WithdrawnAccount? {
        return withdrawnAccountRepository.findByProviderAndEmail(
            provider = AuthProvider.EMAIL,
            email = email,
        )
    }

    private fun createNewKakaoUser(
        providerId: String,
        email: String,
        nickname: String,
    ): User {
        val sanitizedNickname = sanitizeKakaoNicknameForPreload(nickname)
        val newUser = User.newKakaoUser(
            providerId = providerId,
            email = email,
            nickname = sanitizedNickname,
        )
        val savedUser = userRepository.save(newUser)
        log.info("[UserService] 신규 카카오 사용자 생성 완료: userId={}", savedUser.id)
        return savedUser
    }

    private fun sanitizeKakaoNicknameForPreload(rawNickname: String): String? {
        val nickname = rawNickname.trim()
        return if (NICKNAME_REGEX.matches(nickname)) nickname else null
    }

    private fun validateRejoinAvailable(rejoinAvailableAt: LocalDateTime) {
        if (LocalDateTime.now().isBefore(rejoinAvailableAt)) {
            throw CustomException(ErrorCode.REJOIN_NOT_AVAILABLE_YET)
        }
    }

    private fun validateKakaoRejoinAvailable(providerId: String) {
        val withdrawnAccount = findWithdrawnKakaoAccount(providerId) ?: return
        validateRejoinAvailable(withdrawnAccount.rejoinAvailableAt)
    }

    private fun validateEmailRejoinAvailable(email: String) {
        val withdrawnAccount = findWithdrawnEmailAccount(email) ?: return
        validateRejoinAvailable(withdrawnAccount.rejoinAvailableAt)
    }

    private fun isEmailRejoinBlocked(email: String): Boolean {
        val withdrawnAccount = findWithdrawnEmailAccount(email) ?: return false
        return LocalDateTime.now().isBefore(withdrawnAccount.rejoinAvailableAt)
    }

    private fun validateNicknameFormat(nickname: String) {
        if (!NICKNAME_REGEX.matches(nickname)) {
            throw CustomException(ErrorCode.INVALID_NICKNAME_FORMAT)
        }
    }

    private fun validatePhoneNumberFormat(phoneNumber: String) {
        val phoneRegex = Regex("^01[0-9]-[0-9]{3,4}-[0-9]{4}$")
        if (!phoneRegex.matches(phoneNumber)) {
            throw CustomException(ErrorCode.INVALID_PHONE_NUMBER_FORMAT)
        }
    }

    private fun assertNoDuplicateNickname(nickname: String, userId: Long) {
        if (userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(nickname, userId)) {
            throw CustomException(ErrorCode.DUPLICATE_NICKNAME)
        }
    }

    private fun isNicknameDuplicated(nickname: String, userId: Long?): Boolean {
        if (userId == null) {
            return userRepository.existsByNicknameAndDeletedAtIsNull(nickname)
        }

        return userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull(nickname, userId)
    }

    private fun validateEmailFormat(email: String) {
        if (!EMAIL_REGEX.matches(email)) {
            throw CustomException(ErrorCode.INVALID_EMAIL_FORMAT)
        }
    }

    private fun validatePasswordPolicyForChange(email: String, newPassword: String) {
        if (!PASSWORD_REGEX.matches(newPassword)) {
            throw CustomException(ErrorCode.INVALID_PASSWORD_FORMAT)
        }

        val emailLocalPart = email.substringBefore("@")
        if (emailLocalPart.equals(newPassword, ignoreCase = true)) {
            throw CustomException(ErrorCode.INVALID_PASSWORD_SAME_AS_EMAIL_ID)
        }
    }

    private fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*[0-9]).{8,20}$")
        private val NICKNAME_REGEX = Regex("^[A-Za-z0-9가-힣]{2,10}$")
    }
}
