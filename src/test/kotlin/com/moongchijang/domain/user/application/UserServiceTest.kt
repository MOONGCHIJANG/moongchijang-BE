package com.moongchijang.domain.user.application

import com.moongchijang.domain.auth.application.PhoneVerificationService
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
    private val userService = UserService(userRepository, phoneVerificationService)

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
}
