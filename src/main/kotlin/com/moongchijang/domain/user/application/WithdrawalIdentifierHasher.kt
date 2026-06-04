package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.domain.entity.AuthProvider
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class WithdrawalIdentifierHasher {

    fun hashEmail(email: String): String {
        val normalizedEmail = email.trim().lowercase()
        return sha256("${AuthProvider.EMAIL.name}:$normalizedEmail")
    }

    fun hashProviderIdentifier(provider: AuthProvider, providerIdentifier: String): String {
        return sha256("${provider.name}:${providerIdentifier.trim()}")
    }

    fun hashForWithdrawal(provider: AuthProvider, providerId: String?, email: String?): String? {
        val normalizedProviderId = providerId?.trim()?.takeIf { it.isNotEmpty() }
        if (normalizedProviderId != null) {
            return hashProviderIdentifier(provider, normalizedProviderId)
        }

        val normalizedEmail = email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (normalizedEmail != null) {
            return sha256("${provider.name}:$normalizedEmail")
        }

        return null
    }

    private fun sha256(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
