package com.moongchijang.security.crypto

import org.springframework.stereotype.Component

@Component
class PersonalInfoManager(
    private val encryptor: PersonalInfoEncryptor,
    private val hasher: PersonalInfoHasher,
) {
    fun encryptEmail(normalizedEmail: String): String = encryptor.encrypt(normalizedEmail)

    fun encryptPhone(phoneNumber: String): String = encryptor.encrypt(phoneNumber)

    fun hashEmail(normalizedEmail: String): String = hasher.hash(normalizedEmail)

    fun isEncrypted(value: String): Boolean = encryptor.isEncrypted(value)

    fun decryptIfNeeded(value: String?): String? {
        if (value.isNullOrBlank()) {
            return value
        }
        return if (encryptor.isEncrypted(value)) encryptor.decrypt(value) else value
    }
}
