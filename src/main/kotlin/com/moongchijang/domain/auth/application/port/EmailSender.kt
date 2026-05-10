package com.moongchijang.domain.auth.application.port

interface EmailSender {
    fun sendVerificationCode(toEmail: String, code: String, expiresInSeconds: Long)
}
