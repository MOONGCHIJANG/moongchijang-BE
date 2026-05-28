package com.moongchijang.domain.auth.application.port

interface PhoneVerificationStore {
    fun saveCodeHash(phoneNumber: String, codeHash: String, ttlSeconds: Long)
    fun getCodeHash(phoneNumber: String): String?
    fun deleteCode(phoneNumber: String)

    fun markVerified(phoneNumber: String, ttlSeconds: Long)
    fun isVerified(phoneNumber: String): Boolean
    fun markVerifiedForUser(userId: Long, phoneNumber: String, ttlSeconds: Long)
    fun isVerifiedForUser(userId: Long, phoneNumber: String): Boolean
}
