package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService(
    private val userRepository: UserRepository
) {

    @Transactional
    fun findOrCreateKakaoUser(
        providerId: String,
        email: String,
        nickname: String,
    ): Pair<User, Boolean> {
        findActiveKakaoUser(providerId)?.let { return it to false }
        findDeletedKakaoUser(providerId)?.let { return restoreDeletedUser(it, email, nickname) to false }
        return createNewKakaoUser(providerId, email, nickname) to true
    }

    @Transactional(readOnly = true)
    fun existsByNickname(nickname: String): Boolean {
        return userRepository.existsByNicknameAndDeletedAtIsNull(nickname)
    }

    @Transactional(readOnly = true)
    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmailAndDeletedAtIsNull(email)
    }

    private fun findActiveKakaoUser(providerId: String): User? {
        return userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(
            provider = AuthProvider.KAKAO,
            providerId = providerId,
        )
    }

    private fun findDeletedKakaoUser(providerId: String): User? {
        return userRepository.findByProviderAndProviderIdAndDeletedAtIsNotNull(
            provider = AuthProvider.KAKAO,
            providerId = providerId,
        )
    }

    private fun restoreDeletedUser(
        deletedUser: User,
        email: String,
        nickname: String,
    ): User {
        val deletedAt = deletedUser.deletedAt
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        validateRejoinAvailable(deletedAt)

        deletedUser.deletedAt = null
        deletedUser.email = email
        if (deletedUser.nickname.isNullOrBlank()) {
            deletedUser.nickname = nickname
        }
        deletedUser.signupCompleted = false

        return deletedUser
    }

    private fun createNewKakaoUser(
        providerId: String,
        email: String,
        nickname: String,
    ): User {
        val newUser = User.newKakaoUser(
            providerId = providerId,
            email = email,
            nickname = nickname,
        )
        return userRepository.save(newUser)
    }

    private fun validateRejoinAvailable(deletedAt: LocalDateTime) {
        val rejoinAvailableAt = deletedAt.plusDays(30)
        if (LocalDateTime.now().isBefore(rejoinAvailableAt)) {
            throw CustomException(ErrorCode.REJOIN_NOT_AVAILABLE_YET)
        }
    }
}
