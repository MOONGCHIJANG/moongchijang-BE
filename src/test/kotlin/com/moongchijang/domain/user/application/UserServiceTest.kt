package com.moongchijang.domain.user.application

import com.moongchijang.domain.auth.application.PhoneVerificationService
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
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.LocalDateTime

class UserServiceTest {

    private val userRepository: UserRepository = Mockito.mock(UserRepository::class.java)
    private val phoneVerificationService: PhoneVerificationService =
        Mockito.mock(PhoneVerificationService::class.java)
    private val participationRepository: ParticipationRepository = Mockito.mock(ParticipationRepository::class.java)
    private val favoriteRepository: FavoriteRepository = Mockito.mock(FavoriteRepository::class.java)
    private val paymentService: PaymentService = Mockito.mock(PaymentService::class.java)
    private val userService = UserService(
        userRepository,
        phoneVerificationService,
        participationRepository,
        favoriteRepository,
        paymentService,
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
        Mockito.`when`(userRepository.existsByNicknameAndDeletedAtIsNull("중복닉네임")).thenReturn(true)

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

        userService.withdraw(1L)

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

        userService.withdraw(2L)

        Mockito.verifyNoInteractions(paymentService)
        Mockito.verify(favoriteRepository).deleteByUserId(2L)
        Assertions.assertEquals(true, user.deletedAt != null)
    }
}
