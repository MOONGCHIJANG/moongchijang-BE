package com.moongchijang.domain.auth.application.port

interface EmailSignupTokenStore {
    fun save(email: String, signupToken: String, expiresInSeconds: Long)
    fun isValid(email: String, signupToken: String): Boolean
    fun delete(email: String)
}
