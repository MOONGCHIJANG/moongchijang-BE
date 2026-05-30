package com.moongchijang.domain.user.application

import com.moongchijang.domain.auth.application.PhoneVerificationService
import com.moongchijang.domain.auth.application.TokenService
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.payment.application.PaymentService
import com.moongchijang.domain.payment.application.dto.CancelParticipationRequest
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpsertRequest
import com.moongchijang.domain.user.application.dto.NicknameUpdateRequest
import com.moongchijang.domain.user.application.dto.PasswordChangeRequest
import com.moongchijang.domain.user.application.dto.PhoneNumberUpdateRequest
import com.moongchijang.domain.user.application.dto.WithdrawRequest
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.entity.WithdrawalReason
import com.moongchijang.domain.user.domain.entity.SellerBusinessProfile
import com.moongchijang.domain.user.domain.entity.SellerSettlementAccount
import com.moongchijang.domain.user.domain.repository.SellerBusinessProfileRepository
import com.moongchijang.domain.user.domain.repository.SellerSettlementAccountRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

class UserServiceTest {

    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val sellerBusinessProfileRepository: SellerBusinessProfileRepository =
        Mockito.mock(SellerBusinessProfileRepository::class.java)
    private val sellerSettlementAccountRepository: SellerSettlementAccountRepository =
        Mockito.mock(SellerSettlementAccountRepository::class.java)
    private val phoneVerificationService: PhoneVerificationService =
        Mockito.mock(PhoneVerificationService::class.java)
    private val tokenService: TokenService = Mockito.mock(TokenService::class.java)
    private val participationRepository: ParticipationRepository = Mockito.mock(ParticipationRepository::class.java)
    private val favoriteRepository: FavoriteRepository = Mockito.mock(FavoriteRepository::class.java)
    private val paymentService: PaymentService = Mockito.mock(PaymentService::class.java)
    private val passwordEncoder: PasswordEncoder = Mockito.mock(PasswordEncoder::class.java)
    private val userService = UserService(
        userRepository,
        sellerBusinessProfileRepository,
        sellerSettlementAccountRepository,
        phoneVerificationService,
        tokenService,
        participationRepository,
        favoriteRepository,
        paymentService,
        passwordEncoder,
    )

    @Test
    fun `활성 카카오 사용자 존재 시 기존 사용자 반환`() {
        val existingUser = UserFixture.createKakaoUser(id = 1L, providerId = "kakao-1", nickname = "기존닉네임")

        Mockito.`when`(
            userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(AuthProvider.KAKAO, "kakao-1"),
        ).thenReturn(existingUser)

        val (user, isNewUser) = userService.findOrCreateKakaoUser(
            providerId = "kakao-1",
            email = "test@example.com",
            nickname = "신규닉네임",
        )

        Assertions.assertEquals(1L, user.id)
        Assertions.assertFalse(isNewUser)
        Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User::class.java))
    }

    @Test
    fun `활성 사용자 및 탈퇴 사용자 부재 시 신규 사용자 생성`() {
        val savedUser = UserFixture.createKakaoUser(
            id = 10L,
            providerId = "kakao-new",
            email = "new@example.com",
            nickname = "신규유저"
        )

        Mockito.`when`(
            userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(AuthProvider.KAKAO, "kakao-new"),
        ).thenReturn(null)
        Mockito.`when`(
            userRepository.findByProviderAndProviderIdAndDeletedAtIsNotNull(AuthProvider.KAKAO, "kakao-new"),
        ).thenReturn(null)
        Mockito.`when`(userRepository.save(Mockito.any(User::class.java))).thenReturn(savedUser)

        val (user, isNewUser) = userService.findOrCreateKakaoUser(
            providerId = "kakao-new",
            email = "new@example.com",
            nickname = "신규유저",
        )

        Assertions.assertEquals(10L, user.id)
        Assertions.assertTrue(isNewUser)
        Mockito.verify(userRepository).save(Mockito.any(User::class.java))
    }

    @Test
    fun `추가정보 입력 시 닉네임 형식 오류 예외`() {
        val exception = assertThrows<CustomException> {
            userService.updateAdditionalInfo(
                request = AdditionalInfoUpsertRequest(
                    nickname = "닉 네 임",
                    phoneNumber = "010-1234-5678",
                ),
                userId = 1L,
            )
        }

        Assertions.assertEquals(ErrorCode.INVALID_NICKNAME_FORMAT, exception.errorCode)
    }

    @Test
    fun `추가정보 입력 시 닉네임 중복 예외`() {
        val user = UserFixture.createKakaoUser(
            id = 1L,
            providerId = "kakao-dup",
            email = "dup@example.com",
            nickname = "기존닉네임"
        )

        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(user)
        Mockito.`when`(userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("중복닉네임", 1L)).thenReturn(true)

        val exception = assertThrows<CustomException> {
            userService.updateAdditionalInfo(
                request = AdditionalInfoUpsertRequest(
                    nickname = "중복닉네임",
                    phoneNumber = "010-1234-5678",
                ),
                userId = 1L,
            )
        }

        Assertions.assertEquals(ErrorCode.DUPLICATE_NICKNAME, exception.errorCode)
    }

    @Test
    fun `탈퇴일 기준 30일 미만 재가입 불가 예외`() {
        val deletedUser = UserFixture.createKakaoUser(
            id = 20L,
            providerId = "kakao-deleted",
            email = "deleted@example.com",
            nickname = "탈퇴유저",
            deletedAt = LocalDateTime.now().minusDays(5),
        )

        Mockito.`when`(
            userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(AuthProvider.KAKAO, "kakao-deleted"),
        ).thenReturn(null)
        Mockito.`when`(
            userRepository.findByProviderAndProviderIdAndDeletedAtIsNotNull(AuthProvider.KAKAO, "kakao-deleted"),
        ).thenReturn(deletedUser)

        val exception = assertThrows<CustomException> {
            userService.findOrCreateKakaoUser(
                providerId = "kakao-deleted",
                email = "deleted@example.com",
                nickname = "새닉네임",
            )
        }

        Assertions.assertEquals(ErrorCode.REJOIN_NOT_AVAILABLE_YET, exception.errorCode)
    }

    @Test
    fun `이메일 중복 확인 시 사용 가능하면 true`() {
        Mockito.`when`(
            userRepository.existsByProviderAndEmailAndDeletedAtIsNull(AuthProvider.EMAIL, "new@example.com"),
        ).thenReturn(false)

        val response = userService.checkEmailAvailability("new@example.com")

        Assertions.assertTrue(response.available)
        Assertions.assertEquals("new@example.com", response.email)
    }

    @Test
    fun `닉네임 중복 확인 시 로그인 사용자 본인 닉네임이면 사용 가능`() {
        Mockito.`when`(userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("내닉네임", 61L)).thenReturn(false)

        val response = userService.checkNicknameAvailability("내닉네임", 61L)

        Assertions.assertTrue(response.available)
        Assertions.assertEquals("내닉네임", response.nickname)
    }

    @Test
    fun `닉네임 중복 확인 시 비로그인 사용자는 중복 닉네임이면 사용 불가`() {
        Mockito.`when`(userRepository.existsByNicknameAndDeletedAtIsNull("중복닉네임")).thenReturn(true)

        val response = userService.checkNicknameAvailability("중복닉네임", null)

        Assertions.assertFalse(response.available)
        Assertions.assertEquals("중복닉네임", response.nickname)
    }

    @Test
    fun `이메일 중복 확인 시 중복이면 false`() {
        Mockito.`when`(
            userRepository.existsByProviderAndEmailAndDeletedAtIsNull(AuthProvider.EMAIL, "dup@example.com"),
        ).thenReturn(true)

        val response = userService.checkEmailAvailability("dup@example.com")

        Assertions.assertFalse(response.available)
        Assertions.assertEquals("dup@example.com", response.email)
    }

    @Test
    fun `이메일 중복 확인 시 형식 오류 예외`() {
        val exception = assertThrows<CustomException> {
            userService.checkEmailAvailability("invalid-email")
        }

        Assertions.assertEquals(ErrorCode.INVALID_EMAIL_FORMAT, exception.errorCode)
    }

    @Test
    fun `이메일 사용자 생성 성공`() {
        val savedUser = UserFixture.createEmailUser(
            id = 30L,
            email = "new@example.com",
            passwordHash = "hashed-password",
        )

        Mockito.`when`(
            userRepository.existsByProviderAndEmailAndDeletedAtIsNull(AuthProvider.EMAIL, "new@example.com"),
        ).thenReturn(false)
        Mockito.`when`(userRepository.save(Mockito.any(User::class.java))).thenReturn(savedUser)

        val user = userService.createEmailUser("new@example.com", "hashed-password")

        Assertions.assertEquals(30L, user.id)
        Assertions.assertEquals(AuthProvider.EMAIL, user.provider)
        Assertions.assertEquals("new@example.com", user.email)
        Assertions.assertEquals("hashed-password", user.passwordHash)
    }

    @Test
    fun `이메일 사용자 생성 시 중복 이메일 예외`() {
        Mockito.`when`(
            userRepository.existsByProviderAndEmailAndDeletedAtIsNull(AuthProvider.EMAIL, "dup@example.com"),
        ).thenReturn(true)

        val exception = assertThrows<CustomException> {
            userService.createEmailUser("dup@example.com", "hashed-password")
        }

        Assertions.assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.errorCode)
    }

    @Test
    fun `이메일 사용자 생성 시 이메일 형식 오류 예외`() {
        val exception = assertThrows<CustomException> {
            userService.createEmailUser("invalid-email", "hashed-password")
        }

        Assertions.assertEquals(ErrorCode.INVALID_EMAIL_FORMAT, exception.errorCode)
    }

    @Test
    fun `이메일 사용자 조회 성공`() {
        val user = UserFixture.createEmailUser(
            id = 31L,
            email = "login@example.com",
            passwordHash = "hashed-password",
        )
        Mockito.`when`(
            userRepository.findByProviderAndEmailAndDeletedAtIsNull(AuthProvider.EMAIL, "login@example.com"),
        ).thenReturn(user)

        val found = userService.findActiveEmailUser("login@example.com")

        Assertions.assertEquals(31L, found?.id)
        Assertions.assertEquals("login@example.com", found?.email)
    }

    @Test
    fun `이메일 사용자 조회 시 이메일 형식 오류 예외`() {
        val exception = assertThrows<CustomException> {
            userService.findActiveEmailUser("invalid-email")
        }

        Assertions.assertEquals(ErrorCode.INVALID_EMAIL_FORMAT, exception.errorCode)
    }

    @Test
    fun `내 정보 조회 성공`() {
        val user = UserFixture.createKakaoUser(id = 41L, providerId = "kakao-41", nickname = "조회유저").apply {
            role = UserRole.SELLER
            lastRole = UserRole.SELLER
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.BUYER))
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.SELLER))
        }
        setAuditFields(user, LocalDateTime.now(), LocalDateTime.now())
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(41L)).thenReturn(user)

        val response = userService.getMyInfo(41L)

        Assertions.assertEquals(41L, response.id)
        Assertions.assertEquals(user.role, response.role)
        Assertions.assertEquals(user.lastRole, response.lastRole)
        Assertions.assertTrue(response.hasSellerRole)
        Assertions.assertTrue(response.canSwitchToBuyer)
        Assertions.assertFalse(response.canSwitchToSeller)
    }

    @Test
    fun `내 정보 조회 사용자 없음 예외`() {
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(42L)).thenReturn(null)

        val exception = assertThrows<CustomException> {
            userService.getMyInfo(42L)
        }

        Assertions.assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `닉네임 변경할 때 정상 요청이면 닉네임이 변경됨`() {
        val user = UserFixture.createKakaoUser(id = 51L, providerId = "kakao-51", nickname = "기존닉네임")
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(51L)).thenReturn(user)
        Mockito.`when`(userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("새닉네임", 51L)).thenReturn(false)

        val response = userService.updateNickname(
            request = NicknameUpdateRequest(nickname = "새닉네임"),
            userId = 51L,
        )

        Assertions.assertEquals("새닉네임", user.nickname)
        Assertions.assertEquals(51L, response.id)
        Assertions.assertEquals("새닉네임", response.nickname)
    }

    @Test
    fun `닉네임 변경할 때 중복 닉네임이면 예외`() {
        val user = UserFixture.createKakaoUser(id = 52L, providerId = "kakao-52", nickname = "기존닉네임")
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(52L)).thenReturn(user)
        Mockito.`when`(userRepository.existsByNicknameAndIdNotAndDeletedAtIsNull("중복닉네임", 52L)).thenReturn(true)

        val exception = assertThrows<CustomException> {
            userService.updateNickname(
                request = NicknameUpdateRequest(nickname = "중복닉네임"),
                userId = 52L,
            )
        }

        Assertions.assertEquals(ErrorCode.DUPLICATE_NICKNAME, exception.errorCode)
    }

    @Test
    fun `전화번호 변경할 때 인증 완료 번호면 전화번호가 변경됨`() {
        val user = UserFixture.createKakaoUser(id = 53L, providerId = "kakao-53", nickname = "유저")
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(53L)).thenReturn(user)

        val response = userService.updatePhoneNumber(
            request = PhoneNumberUpdateRequest(phoneNumber = "010-9999-8888"),
            userId = 53L,
        )

        Mockito.verify(phoneVerificationService).ensureVerifiedForUser(53L, "010-9999-8888")
        Assertions.assertEquals("010-9999-8888", user.phoneNumber)
        Assertions.assertEquals(53L, response.id)
        Assertions.assertEquals("010-9999-8888", response.phoneNumber)
    }

    @Test
    fun `비밀번호 변경할 때 이메일 사용자가 아니면 예외`() {
        val user = UserFixture.createKakaoUser(id = 54L, providerId = "kakao-54", nickname = "유저")
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(54L)).thenReturn(user)

        val exception = assertThrows<CustomException> {
            userService.changePassword(
                request = PasswordChangeRequest(
                    currentPassword = "oldPassword1",
                    newPassword = "newPassword1",
                ),
                userId = 54L,
            )
        }

        Assertions.assertEquals(ErrorCode.EMAIL_PASSWORD_CHANGE_NOT_ALLOWED, exception.errorCode)
    }

    @Test
    fun `비밀번호 변경할 때 현재 비밀번호가 다르면 예외`() {
        val user = UserFixture.createEmailUser(
            id = 55L,
            email = "login@example.com",
            passwordHash = "encoded-old",
        )
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(55L)).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches("wrongPassword1", "encoded-old")).thenReturn(false)

        val exception = assertThrows<CustomException> {
            userService.changePassword(
                request = PasswordChangeRequest(
                    currentPassword = "wrongPassword1",
                    newPassword = "newPassword1",
                ),
                userId = 55L,
            )
        }

        Assertions.assertEquals(ErrorCode.PASSWORD_CHANGE_CURRENT_PASSWORD_MISMATCH, exception.errorCode)
    }

    @Test
    fun `비밀번호 변경할 때 정책을 만족하면 비밀번호가 변경되고 토큰이 제거됨`() {
        val user = UserFixture.createEmailUser(
            id = 56L,
            email = "login@example.com",
            passwordHash = "encoded-old",
        )
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(56L)).thenReturn(user)
        Mockito.`when`(passwordEncoder.matches("oldPassword1", "encoded-old")).thenReturn(true)
        Mockito.`when`(passwordEncoder.encode("newPassword1")).thenReturn("encoded-new")

        val response = userService.changePassword(
            request = PasswordChangeRequest(
                currentPassword = "oldPassword1",
                newPassword = "newPassword1",
            ),
            userId = 56L,
        )

        Assertions.assertEquals("encoded-new", user.passwordHash)
        Assertions.assertTrue(response.changed)
        Mockito.verify(tokenService).deleteByUserId(56L)
    }

    @Test
    fun `회원탈퇴 불가 예외`() {
        Mockito.`when`(
            participationRepository.existsPendingPickupForWithdrawal(
                1L,
                ParticipationStatus.CONFIRMED,
                listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
            )
        ).thenReturn(true)

        val exception = assertThrows<CustomException> {
            userService.validateWithdrawable(1L)
        }

        Assertions.assertEquals(ErrorCode.WITHDRAWAL_BLOCKED_PENDING_PICKUP, exception.errorCode)
    }

    @Test
    fun `회원탈퇴 성공 처리`() {
        val user = UserFixture.createKakaoUser(id = 1L, providerId = "kakao-1", nickname = "탈퇴대상")
        val participation = Mockito.mock(Participation::class.java)
        Mockito.`when`(participation.id).thenReturn(100L)

        Mockito.`when`(
            participationRepository.existsPendingPickupForWithdrawal(
                1L,
                ParticipationStatus.CONFIRMED,
                listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
            )
        ).thenReturn(false)
        Mockito.`when`(
            participationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, ParticipationStatus.PAID_WAITING_GOAL)
        ).thenReturn(listOf(participation))
        Mockito.`when`(favoriteRepository.deleteByUserId(1L)).thenReturn(3)
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(user)

        userService.withdraw(
            1L,
            WithdrawRequest(
                reason = WithdrawalReason.NO_DESIRED_GROUPBUY,
                reasonDetail = null,
            )
        )

        Mockito.verify(paymentService).cancelParticipation(
            100L,
            1L,
            CancelParticipationRequest(
                reason = ParticipationCancelReason.OTHER,
                reasonDetail = "회원탈퇴 자동 취소",
            )
        )
        Mockito.verify(favoriteRepository).deleteByUserId(1L)
        Assertions.assertEquals(true, user.deletedAt != null)
    }

    @Test
    fun `회원탈퇴 참여중 공구 없음 처리`() {
        val user = UserFixture.createKakaoUser(id = 2L, providerId = "kakao-2", nickname = "탈퇴대상2")

        Mockito.`when`(
            participationRepository.existsPendingPickupForWithdrawal(
                2L,
                ParticipationStatus.CONFIRMED,
                listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
            )
        ).thenReturn(false)
        Mockito.`when`(
            participationRepository.findByUserIdAndStatusOrderByCreatedAtDesc(2L, ParticipationStatus.PAID_WAITING_GOAL)
        ).thenReturn(emptyList())
        Mockito.`when`(favoriteRepository.deleteByUserId(2L)).thenReturn(0)
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(user)

        userService.withdraw(
            2L,
            WithdrawRequest(
                reason = WithdrawalReason.INCONVENIENT_SERVICE,
                reasonDetail = null,
            )
        )

        Mockito.verifyNoInteractions(paymentService)
        Mockito.verify(favoriteRepository).deleteByUserId(2L)
        Assertions.assertEquals(true, user.deletedAt != null)
    }

    @Test
    fun `회원탈퇴 기타 사유 상세 미입력 예외`() {
        val user = UserFixture.createKakaoUser(id = 1L, providerId = "kakao-1", nickname = "탈퇴대상")
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(user)

        val exception = assertThrows<CustomException> {
            userService.withdraw(
                1L,
                WithdrawRequest(
                    reason = WithdrawalReason.OTHER,
                    reasonDetail = "   ",
                )
            )
        }

        Assertions.assertEquals(ErrorCode.WITHDRAWAL_REASON_DETAIL_REQUIRED, exception.errorCode)
    }

    @Test
    fun `회원탈퇴 컨텍스트 가능 이후 실행 시점 차단 예외`() {
        val user = UserFixture.createKakaoUser(id = 55L, providerId = "kakao-55", nickname = "경합유저")
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(55L)).thenReturn(user)
        Mockito.`when`(
            participationRepository.existsPendingPickupForWithdrawal(
                55L,
                ParticipationStatus.CONFIRMED,
                listOf(PickupStatus.NOT_READY, PickupStatus.READY),
                listOf(GroupBuyStatus.ACHIEVED, GroupBuyStatus.COMPLETED),
            )
        ).thenReturn(true)

        val exception = assertThrows<CustomException> {
            userService.withdraw(
                55L,
                WithdrawRequest(
                    reason = WithdrawalReason.INCONVENIENT_SERVICE,
                    reasonDetail = null,
                )
            )
        }

        Assertions.assertEquals(ErrorCode.WITHDRAWAL_BLOCKED_PENDING_PICKUP, exception.errorCode)
    }

    @Test
    fun `회원탈퇴 사장님 권한 사용자 차단 예외`() {
        val sellerUser = UserFixture.createKakaoUser(id = 56L, providerId = "kakao-56", nickname = "사장님").apply {
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.SELLER))
        }
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(56L)).thenReturn(sellerUser)

        val exception = assertThrows<CustomException> {
            userService.withdraw(
                56L,
                WithdrawRequest(
                    reason = WithdrawalReason.INCONVENIENT_SERVICE,
                    reasonDetail = null,
                )
            )
        }

        Assertions.assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    fun `사장님 사업자 정보 저장할 때 사업자 정보가 저장됨`() {
        val user = UserFixture.createKakaoUser(id = 101L, providerId = "kakao-101")
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(101L)).thenReturn(user)
        Mockito.`when`(sellerBusinessProfileRepository.findByUserId(101L)).thenReturn(null)

        userService.upsertSellerBusinessInfo(
            request = com.moongchijang.domain.user.application.dto.SellerBusinessInfoUpsertRequest(
                businessRegistrationNumber = "111-22-33333",
                storeName = "뭉치장베이커리",
                ownerName = "홍길동",
                storeAddress = "서울시 강남구 테헤란로 123, 2층",
                phoneNumber = "010-1234-5678",
            ),
            userId = 101L,
        )

        val captor = org.mockito.ArgumentCaptor.forClass(SellerBusinessProfile::class.java)
        Mockito.verify(sellerBusinessProfileRepository).save(captor.capture())
        Assertions.assertEquals("1112233333", captor.value.businessRegistrationNumber)
        Assertions.assertEquals("뭉치장베이커리", captor.value.storeName)
        Assertions.assertEquals("홍길동", captor.value.ownerName)
    }

    @Test
    fun `사장님 정산 정보 저장할 때 SELLER 권한과 가입 완료 상태가 반영됨`() {
        val user = UserFixture.createKakaoUser(id = 102L, providerId = "kakao-102")
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(102L)).thenReturn(user)
        Mockito.`when`(sellerBusinessProfileRepository.existsByUserId(102L)).thenReturn(true)
        Mockito.`when`(sellerSettlementAccountRepository.findByUserId(102L)).thenReturn(null)

        val response = userService.upsertSellerSettlementInfo(
            request = com.moongchijang.domain.user.application.dto.SellerSettlementInfoUpsertRequest(
                bankCode = "KB국민",
                accountNumber = "000-000-0000",
                accountHolderName = "홍길동",
            ),
            userId = 102L,
        )

        val captor = org.mockito.ArgumentCaptor.forClass(SellerSettlementAccount::class.java)
        Mockito.verify(sellerSettlementAccountRepository).save(captor.capture())
        Assertions.assertEquals("KOOKMIN", captor.value.bankCode)
        Assertions.assertEquals(UserRole.SELLER, user.role)
        Assertions.assertTrue(user.sellerSignupCompleted)
        Assertions.assertTrue(user.hasRole(UserRole.SELLER))
        Assertions.assertTrue(response.sellerSignupCompleted)
    }

    @Test
    fun `사장님 정산 정보 저장 시 사업자 정보가 없으면 예외`() {
        val user = UserFixture.createKakaoUser(id = 103L, providerId = "kakao-103")
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(103L)).thenReturn(user)
        Mockito.`when`(sellerBusinessProfileRepository.existsByUserId(103L)).thenReturn(false)

        val exception = assertThrows<CustomException> {
            userService.upsertSellerSettlementInfo(
                request = com.moongchijang.domain.user.application.dto.SellerSettlementInfoUpsertRequest(
                    bankCode = "KB국민",
                    accountNumber = "000-000-0000",
                    accountHolderName = "홍길동",
                ),
                userId = 103L,
            )
        }

        Assertions.assertEquals(ErrorCode.SELLER_BUSINESS_INFO_REQUIRED, exception.errorCode)
    }

    @Test
    fun `마이페이지 역할 전환 성공`() {
        val sellerUser = UserFixture.createKakaoUser(id = 200L, providerId = "kakao-200", nickname = "겸용유저").apply {
            role = UserRole.BUYER
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.BUYER))
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.SELLER))
        }
        setAuditFields(sellerUser, LocalDateTime.now(), LocalDateTime.now())
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(200L)).thenReturn(sellerUser)

        val response = userService.switchMyPageRole(200L, UserRole.SELLER)

        Assertions.assertEquals(UserRole.SELLER, response.role)
        Assertions.assertEquals(UserRole.SELLER, response.lastRole)
        Assertions.assertTrue(response.hasSellerRole)
        Assertions.assertTrue(response.canSwitchToBuyer)
        Assertions.assertFalse(response.canSwitchToSeller)
    }

    @Test
    fun `마이페이지 역할 전환 사장님 권한 없음 예외`() {
        val buyerUser = UserFixture.createKakaoUser(id = 201L, providerId = "kakao-201", nickname = "소비자")
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(201L)).thenReturn(buyerUser)

        val exception = assertThrows<CustomException> {
            userService.switchMyPageRole(201L, UserRole.SELLER)
        }

        Assertions.assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    fun `사장님 입금 계좌 조회 성공`() {
        val sellerUser = UserFixture.createKakaoUser(id = 202L, providerId = "kakao-202").apply {
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.SELLER))
        }
        val account = SellerSettlementAccount(
            user = sellerUser,
            bankCode = "KOOKMIN",
            accountNumber = "000-000-0000",
            accountHolderName = "홍길동",
        )
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(202L)).thenReturn(sellerUser)
        Mockito.`when`(sellerSettlementAccountRepository.findByUserId(202L)).thenReturn(account)

        val response = userService.getSellerSettlementAccount(202L)

        Assertions.assertEquals("KOOKMIN", response.bankCode)
        Assertions.assertEquals("000-000-0000", response.accountNumber)
        Assertions.assertEquals("홍길동", response.accountHolderName)
    }

    @Test
    fun `사장님 사업자 정보 변경 성공`() {
        val sellerUser = UserFixture.createKakaoUser(id = 203L, providerId = "kakao-203").apply {
            roleAssignments.add(com.moongchijang.domain.user.domain.entity.UserRoleAssignment(user = this, role = UserRole.SELLER))
        }
        val profile = SellerBusinessProfile(
            user = sellerUser,
            businessRegistrationNumber = "1112233333",
            storeName = "기존상호",
            ownerName = "기존대표",
            storeAddress = "기존주소",
            phoneNumber = "010-0000-0000",
        )
        Mockito.`when`(userRepository.findByIdAndDeletedAtIsNull(203L)).thenReturn(sellerUser)
        Mockito.`when`(sellerBusinessProfileRepository.findByUserId(203L)).thenReturn(profile)

        val response = userService.updateSellerBusinessProfile(
            request = com.moongchijang.domain.user.application.dto.SellerBusinessInfoUpsertRequest(
                businessRegistrationNumber = "999-88-77777",
                storeName = "새상호",
                ownerName = "새대표",
                storeAddress = "새주소",
                phoneNumber = "010-1111-2222",
            ),
            userId = 203L,
        )

        Assertions.assertEquals("9998877777", response.businessRegistrationNumber)
        Assertions.assertEquals("새상호", response.storeName)
        Assertions.assertEquals("새대표", response.ownerName)
        Assertions.assertEquals("새주소", response.storeAddress)
        Assertions.assertEquals("010-1111-2222", response.phoneNumber)
    }

    private fun setAuditFields(user: User, createdAt: LocalDateTime, updatedAt: LocalDateTime) {
        val baseClass = user.javaClass.superclass

        val createdAtField = baseClass.getDeclaredField("createdAt")
        createdAtField.isAccessible = true
        createdAtField.set(user, createdAt)

        val updatedAtField = baseClass.getDeclaredField("updatedAt")
        updatedAtField.isAccessible = true
        updatedAtField.set(user, updatedAt)
    }
}
