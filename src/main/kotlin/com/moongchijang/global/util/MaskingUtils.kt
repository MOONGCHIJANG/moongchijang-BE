package com.moongchijang.global.util

object MaskingUtils {
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"

        val local = parts[0]
        val domain = parts[1]
        val maskedLocal = when {
            local.isEmpty() -> "***"
            local.length <= 2 -> "${local.first()}*"
            else -> local.take(2) + "*".repeat(local.length - 2)
        }

        return "$maskedLocal@$domain"
    }
}
