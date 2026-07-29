package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.domain.entity.WithdrawnAccount
import com.moongchijang.domain.user.domain.repository.WithdrawnAccountRepository
import com.moongchijang.security.crypto.PersonalInfoManager
import com.moongchijang.support.UserFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class WithdrawnAccountCommandServiceTest {

    @Mock
    private lateinit var withdrawnAccountRepository: WithdrawnAccountRepository

    @Mock
    private lateinit var withdrawalIdentifierHasher: WithdrawalIdentifierHasher

    @Mock
    private lateinit var personalInfoManager: PersonalInfoManager

    private val service by lazy {
        WithdrawnAccountCommandService(
            withdrawnAccountRepository = withdrawnAccountRepository,
            withdrawalIdentifierHasher = withdrawalIdentifierHasher,
            personalInfoManager = personalInfoManager,
        )
    }

    @Test
    fun `providerId 기반 해시가 있으면 신규 탈퇴 계정을 생성한다`() {
        val user = UserFixture.createKakaoUser(id = 10L, providerId = "kakao-10", email = "enc-email")
        val withdrawnAt = LocalDateTime.of(2026, 7, 28, 14, 0)
        val captor = ArgumentCaptor.forClass(WithdrawnAccount::class.java)

        `when`(personalInfoManager.decryptIfNeeded("enc-email")).thenReturn("user@example.com")
        `when`(
            withdrawalIdentifierHasher.hashForWithdrawal(
                provider = user.provider,
                providerId = "kakao-10",
                email = "user@example.com",
            )
        ).thenReturn("provider-hash")
        `when`(withdrawnAccountRepository.findByProviderAndIdentifierHash(user.provider, "provider-hash")).thenReturn(null)
        `when`(withdrawnAccountRepository.save(captor.capture())).thenAnswer { it.arguments[0] }

        service.recordWithdrawal(user = user, withdrawnAt = withdrawnAt)

        val saved = captor.value
        assertEquals(user.provider, saved.provider)
        assertEquals("provider-hash", saved.identifierHash)
        assertEquals(10L, saved.withdrawnUserId)
        assertEquals(withdrawnAt, saved.withdrawnAt)
        assertEquals(withdrawnAt.plusDays(30), saved.rejoinAvailableAt)
        verify(withdrawnAccountRepository, never()).findByWithdrawnUserId(10L)
    }

    @Test
    fun `기존 탈퇴 계정이 있으면 재사용하며 탈퇴 시각과 재가입 가능 시각을 갱신한다`() {
        val user = UserFixture.createKakaoUser(id = 11L, providerId = "kakao-11", email = "enc-email")
        val existing = WithdrawnAccount(
            provider = user.provider,
            identifierHash = "old-hash",
            withdrawnUserId = 3L,
            withdrawnAt = LocalDateTime.of(2026, 6, 1, 9, 0),
            rejoinAvailableAt = LocalDateTime.of(2026, 7, 1, 9, 0),
            id = 77L,
        )
        val withdrawnAt = LocalDateTime.of(2026, 7, 28, 15, 0)
        val captor = ArgumentCaptor.forClass(WithdrawnAccount::class.java)

        `when`(personalInfoManager.decryptIfNeeded("enc-email")).thenReturn("user@example.com")
        `when`(
            withdrawalIdentifierHasher.hashForWithdrawal(
                provider = user.provider,
                providerId = "kakao-11",
                email = "user@example.com",
            )
        ).thenReturn("new-hash")
        `when`(withdrawnAccountRepository.findByProviderAndIdentifierHash(user.provider, "new-hash")).thenReturn(existing)
        `when`(withdrawnAccountRepository.save(captor.capture())).thenAnswer { it.arguments[0] }

        service.recordWithdrawal(user = user, withdrawnAt = withdrawnAt)

        assertSame(existing, captor.value)
        assertEquals("new-hash", existing.identifierHash)
        assertEquals(11L, existing.withdrawnUserId)
        assertEquals(withdrawnAt, existing.withdrawnAt)
        assertEquals(withdrawnAt.plusDays(30), existing.rejoinAvailableAt)
    }

    @Test
    fun `providerId가 없으면 이메일 기반 해시로 기존 계정을 조회해 저장한다`() {
        val user = UserFixture.createEmailUser(id = 12L, email = "encrypted-email@example.com")
        val withdrawnAt = LocalDateTime.of(2026, 7, 28, 16, 0)
        val existing = WithdrawnAccount(
            provider = user.provider,
            identifierHash = "email-hash",
            withdrawnUserId = 12L,
            withdrawnAt = withdrawnAt.minusDays(10),
            rejoinAvailableAt = withdrawnAt.plusDays(20),
            id = 91L,
        )

        `when`(personalInfoManager.decryptIfNeeded("encrypted-email@example.com")).thenReturn("email@example.com")
        `when`(
            withdrawalIdentifierHasher.hashForWithdrawal(
                provider = user.provider,
                providerId = null,
                email = "email@example.com",
            )
        ).thenReturn("email-hash")
        `when`(withdrawnAccountRepository.findByProviderAndIdentifierHash(user.provider, "email-hash")).thenReturn(existing)
        `when`(withdrawnAccountRepository.save(existing)).thenReturn(existing)

        service.recordWithdrawal(user = user, withdrawnAt = withdrawnAt)

        verify(withdrawnAccountRepository).findByProviderAndIdentifierHash(user.provider, "email-hash")
        verify(withdrawnAccountRepository, never()).findByWithdrawnUserId(12L)
        assertEquals(withdrawnAt, existing.withdrawnAt)
        assertEquals(withdrawnAt.plusDays(30), existing.rejoinAvailableAt)
    }
}
