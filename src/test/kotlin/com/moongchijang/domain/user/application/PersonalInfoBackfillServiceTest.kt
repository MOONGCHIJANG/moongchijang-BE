package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.security.crypto.AesGcmPersonalInfoEncryptor
import com.moongchijang.security.crypto.HmacSha256PersonalInfoHasher
import com.moongchijang.security.crypto.PersonalInfoEncryptionProperties
import com.moongchijang.security.crypto.PersonalInfoManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.mock

class PersonalInfoBackfillServiceTest {

    private val userRepository: UserRepository = mock(UserRepository::class.java)
    private val personalInfoProperties = PersonalInfoEncryptionProperties(
        secretKey = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    )
    private val personalInfoManager = PersonalInfoManager(
        AesGcmPersonalInfoEncryptor(personalInfoProperties),
        HmacSha256PersonalInfoHasher(personalInfoProperties),
    )
    private val service = PersonalInfoBackfillService(userRepository, personalInfoManager)

    @Test
    fun `plain personal info is encrypted and email hash is filled`() {
        val user = User(
            id = 1L,
            provider = AuthProvider.EMAIL,
            email = "Test@Example.com",
            passwordHash = "hashed",
            phoneNumber = "010-1234-5678",
            role = UserRole.BUYER,
        )

        val updated = service.backfillUser(user)

        assertTrue(updated)
        assertEquals("test@example.com", personalInfoManager.decryptIfNeeded(user.email))
        assertEquals("010-1234-5678", personalInfoManager.decryptIfNeeded(user.phoneNumber))
        assertEquals(personalInfoManager.hashEmail("test@example.com"), user.emailHash)
        assertTrue(personalInfoManager.isEncrypted(user.email!!))
        assertTrue(personalInfoManager.isEncrypted(user.phoneNumber!!))
    }

    @Test
    fun `already encrypted values are skipped while missing email hash is backfilled`() {
        val encryptedEmail = personalInfoManager.encryptEmail("existing@example.com")
        val encryptedPhone = personalInfoManager.encryptPhone("010-9999-8888")
        val user = User(
            id = 2L,
            provider = AuthProvider.EMAIL,
            email = encryptedEmail,
            emailHash = null,
            passwordHash = "hashed",
            phoneNumber = encryptedPhone,
            role = UserRole.BUYER,
        )

        val updated = service.backfillUser(user)

        assertTrue(updated)
        assertEquals(encryptedEmail, user.email)
        assertEquals(encryptedPhone, user.phoneNumber)
        assertEquals(personalInfoManager.hashEmail("existing@example.com"), user.emailHash)
    }

    @Test
    fun `fully backfilled user is skipped`() {
        val encryptedEmail = personalInfoManager.encryptEmail("done@example.com")
        val encryptedPhone = personalInfoManager.encryptPhone("010-0000-0000")
        val user = User(
            id = 3L,
            provider = AuthProvider.EMAIL,
            email = encryptedEmail,
            emailHash = personalInfoManager.hashEmail("done@example.com"),
            passwordHash = "hashed",
            phoneNumber = encryptedPhone,
            role = UserRole.BUYER,
        )

        val updated = service.backfillUser(user)

        assertFalse(updated)
    }
}
