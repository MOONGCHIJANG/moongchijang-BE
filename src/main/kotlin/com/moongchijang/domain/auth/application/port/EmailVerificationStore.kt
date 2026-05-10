package com.moongchijang.domain.auth.application.port

interface EmailVerificationStore {
    fun saveCode(email: String, code: String, expiresInSeconds: Long)
    fun getCode(email: String): String?
    fun deleteCode(email: String)

    fun getResendAvailableInSeconds(email: String): Long
    fun setResendCooldown(email: String, cooldownSeconds: Long)

    fun incrementDailySendCount(email: String): Long
    fun getDailySendCount(email: String): Long
}
