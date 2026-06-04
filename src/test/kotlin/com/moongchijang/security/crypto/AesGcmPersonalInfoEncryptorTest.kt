package com.moongchijang.security.crypto

import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.assertThrows

class AesGcmPersonalInfoEncryptorTest {

    private val encryptor = AesGcmPersonalInfoEncryptor(
        PersonalInfoEncryptionProperties(
            secretKey = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        )
    )

    @Test
    fun `encrypt and decrypt round-trip personal info`() {
        val plainText = "test.user@example.com"

        val encrypted = encryptor.encrypt(plainText)
        val decrypted = encryptor.decrypt(encrypted)

        assertNotEquals(plainText, encrypted)
        assertEquals(plainText, decrypted)
    }

    @Test
    fun `decrypt throws custom exception for tampered cipher text`() {
        val encrypted = encryptor.encrypt("01012345678")
        val tampered = encrypted.dropLast(2) + "ab"

        val exception = assertThrows<CustomException> {
            encryptor.decrypt(tampered)
        }

        assertEquals(ErrorCode.PERSONAL_INFO_DECRYPTION_FAILED, exception.errorCode)
    }

    @Test
    fun `different iv produces different cipher text`() {
        val plainText = "same-value@example.com"

        val first = encryptor.encrypt(plainText)
        val second = encryptor.encrypt(plainText)

        assertNotEquals(first, second)
        assertTrue(first.isNotBlank())
        assertTrue(second.isNotBlank())
    }

    @Test
    fun `invalid base64 secret key throws custom exception`() {
        val exception = assertThrows<CustomException> {
            AesGcmPersonalInfoEncryptor(
                PersonalInfoEncryptionProperties(secretKey = "invalid-base64-secret")
            )
        }

        assertEquals(ErrorCode.PERSONAL_INFO_SECRET_KEY_INVALID, exception.errorCode)
    }

    @Test
    fun `invalid secret key length throws custom exception`() {
        val invalidKey = java.util.Base64.getEncoder().encodeToString("too-short-key".toByteArray())

        val exception = assertThrows<CustomException> {
            AesGcmPersonalInfoEncryptor(
                PersonalInfoEncryptionProperties(secretKey = invalidKey)
            )
        }

        assertEquals(ErrorCode.PERSONAL_INFO_SECRET_KEY_INVALID, exception.errorCode)
    }
}
