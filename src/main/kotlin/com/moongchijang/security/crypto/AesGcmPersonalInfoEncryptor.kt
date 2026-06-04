package com.moongchijang.security.crypto

import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class AesGcmPersonalInfoEncryptor(
    properties: PersonalInfoEncryptionProperties,
) : PersonalInfoEncryptor {

    private val secretKey = SecretKeySpec(decodeSecretKey(properties.secretKey), KEY_ALGORITHM)
    private val secureRandom = SecureRandom()

    override fun encrypt(plainText: String): String {
        return runCatching {
            val iv = ByteArray(IV_LENGTH).also(secureRandom::nextBytes)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))

            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.getEncoder().encodeToString(iv + encrypted)
        }.getOrElse {
            throw CustomException(ErrorCode.PERSONAL_INFO_ENCRYPTION_FAILED)
        }
    }

    override fun decrypt(cipherText: String): String {
        return runCatching {
            val decoded = Base64.getDecoder().decode(cipherText)
            require(decoded.size > IV_LENGTH) { "Cipher text is invalid" }

            val iv = decoded.copyOfRange(0, IV_LENGTH)
            val encrypted = decoded.copyOfRange(IV_LENGTH, decoded.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BIT, iv))

            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrElse {
            throw CustomException(ErrorCode.PERSONAL_INFO_DECRYPTION_FAILED)
        }
    }

    private fun decodeSecretKey(secretKey: String): ByteArray {
        val decoded = try {
            Base64.getDecoder().decode(secretKey)
        } catch (e: IllegalArgumentException) {
            throw CustomException(
                ErrorCode.PERSONAL_INFO_SECRET_KEY_INVALID,
                "security.personal-info.secret-key must be valid Base64"
            )
        }

        if (decoded.size != SECRET_KEY_LENGTH) {
            throw CustomException(
                ErrorCode.PERSONAL_INFO_SECRET_KEY_INVALID,
                "security.personal-info.secret-key must decode to $SECRET_KEY_LENGTH bytes"
            )
        }

        return decoded
    }

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BIT = 128
        private const val SECRET_KEY_LENGTH = 32
    }
}
