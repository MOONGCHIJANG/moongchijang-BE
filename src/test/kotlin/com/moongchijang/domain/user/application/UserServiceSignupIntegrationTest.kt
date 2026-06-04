package com.moongchijang.domain.user.application

import com.moongchijang.domain.auth.application.PhoneVerificationService
import com.moongchijang.domain.user.application.dto.AdditionalInfoUpsertRequest
import com.moongchijang.domain.user.application.dto.SellerBusinessInfoUpsertRequest
import com.moongchijang.domain.user.application.dto.SellerSettlementInfoUpsertRequest
import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.security.crypto.PersonalInfoManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceSignupIntegrationTest {

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var personalInfoManager: PersonalInfoManager

    @MockitoBean
    private lateinit var phoneVerificationService: PhoneVerificationService

    @Test
    fun `소비자 가입 완료 후 사장님 가입 완료 시 두 가입 상태 완료`() {
        val user = persistUser("buyer-then-seller@test.com")

        userService.updateAdditionalInfo(
            request = AdditionalInfoUpsertRequest(
                nickname = "테스터01",
                phoneNumber = "010-1234-5678",
            ),
            userId = user.id!!,
        )

        userService.upsertSellerBusinessInfo(
            request = SellerBusinessInfoUpsertRequest(
                businessRegistrationNumber = "111-22-33333",
                storeName = "뭉치장베이커리",
                ownerName = "홍길동",
                storeAddress = "서울시 강남구 테헤란로 123",
                phoneNumber = "010-1234-5678",
            ),
            userId = user.id!!,
        )
        userService.upsertSellerSettlementInfo(
            request = SellerSettlementInfoUpsertRequest(
                bankCode = "KB국민",
                accountNumber = "000-000-0000",
                accountHolderName = "홍길동",
            ),
            userId = user.id!!,
        )

        val updated = userRepository.findByIdAndDeletedAtIsNull(user.id!!)!!
        assertThat(updated.signupCompleted).isTrue()
        assertThat(updated.sellerSignupCompleted).isTrue()
        assertThat(updated.role).isEqualTo(UserRole.SELLER)
        assertThat(updated.hasRole(UserRole.SELLER)).isTrue()
    }

    @Test
    fun `사장님 가입 완료 후 소비자 가입 완료 시 두 가입 상태 완료`() {
        val user = persistUser("seller-then-buyer@test.com")

        userService.upsertSellerBusinessInfo(
            request = SellerBusinessInfoUpsertRequest(
                businessRegistrationNumber = "222-33-44444",
                storeName = "뭉치장샐러드",
                ownerName = "김사장",
                storeAddress = "서울시 송파구 올림픽로 10",
                phoneNumber = "010-9876-5432",
            ),
            userId = user.id!!,
        )
        userService.upsertSellerSettlementInfo(
            request = SellerSettlementInfoUpsertRequest(
                bankCode = "토스뱅크",
                accountNumber = "111-222-333333",
                accountHolderName = "김사장",
            ),
            userId = user.id!!,
        )

        userService.updateAdditionalInfo(
            request = AdditionalInfoUpsertRequest(
                nickname = "테스터02",
                phoneNumber = "010-9876-5432",
            ),
            userId = user.id!!,
        )

        val updated = userRepository.findByIdAndDeletedAtIsNull(user.id!!)!!
        assertThat(updated.signupCompleted).isTrue()
        assertThat(updated.sellerSignupCompleted).isTrue()
        assertThat(updated.role).isEqualTo(UserRole.SELLER)
        assertThat(updated.hasRole(UserRole.SELLER)).isTrue()
    }

    @Test
    fun `사업자 정보 없이 사장님 정산 정보 저장 시 예외`() {
        val user = persistUser("seller-without-business@test.com")

        val thrown = org.junit.jupiter.api.assertThrows<com.moongchijang.global.exception.CustomException> {
            userService.upsertSellerSettlementInfo(
                request = SellerSettlementInfoUpsertRequest(
                    bankCode = "KB국민",
                    accountNumber = "000-000-0000",
                    accountHolderName = "홍길동",
                ),
                userId = user.id!!,
            )
        }

        assertThat(thrown.errorCode).isEqualTo(com.moongchijang.global.exception.ErrorCode.SELLER_BUSINESS_INFO_REQUIRED)
    }

    @Test
    fun `이메일 회원 생성 시 이메일은 암호화되고 해시는 저장된다`() {
        val created = userService.createEmailUser("encrypt-check@test.com", "hashed-password")

        val saved = userRepository.findByIdAndDeletedAtIsNull(created.id!!)!!

        assertThat(saved.email).isNotEqualTo("encrypt-check@test.com")
        assertThat(personalInfoManager.decryptIfNeeded(saved.email)).isEqualTo("encrypt-check@test.com")
        assertThat(saved.emailHash).isEqualTo(personalInfoManager.hashEmail("encrypt-check@test.com"))
    }

    private fun persistUser(email: String): User {
        return userRepository.save(
            User(
                provider = AuthProvider.EMAIL,
                providerId = null,
                email = email,
                passwordHash = "hashed-password",
                nickname = null,
                phoneNumber = null,
                role = UserRole.BUYER,
                signupCompleted = false,
                sellerSignupCompleted = false,
            ),
        )
    }
}
