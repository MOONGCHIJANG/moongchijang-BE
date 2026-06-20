package com.moongchijang.global.util

object MaskingUtils {
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"

        val local = parts[0]
        val domain = parts[1]
        val maskedLocal = when {
            local.isEmpty() -> "***"
            local.length == 1 -> "${local.first()}*"
            else -> "${local.first()}" + "*".repeat(local.length - 1)
        }

        return "$maskedLocal@$domain"
    }

    fun maskBusinessRegistrationNumber(businessRegistrationNumber: String): String {
        val digitsOnly = businessRegistrationNumber.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length < 4) return "***"
        return "*".repeat((digitsOnly.length - 4).coerceAtLeast(0)) + digitsOnly.takeLast(4)
    }
}
