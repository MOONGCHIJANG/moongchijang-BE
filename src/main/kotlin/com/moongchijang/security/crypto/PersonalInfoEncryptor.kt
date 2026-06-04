package com.moongchijang.security.crypto

interface PersonalInfoEncryptor {
    fun encrypt(plainText: String): String

    fun decrypt(cipherText: String): String
}
