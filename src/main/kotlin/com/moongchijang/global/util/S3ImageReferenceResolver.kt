package com.moongchijang.global.util

import com.moongchijang.global.config.AppS3Properties
import org.springframework.stereotype.Component
import java.net.URI

@Component
class S3ImageReferenceResolver(
    private val appS3Properties: AppS3Properties,
) {
    fun resolve(raw: String): ResolvedImageReference {
        val normalized = raw.trim()
        if (normalized.isBlank()) {
            return ResolvedImageReference(key = null, url = normalized)
        }

        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            val key = extractKeyFromUrl(normalized)
            return ResolvedImageReference(key = key, url = normalized)
        }

        val key = normalized.removePrefix("/")
        val url = buildPublicUrl(key)
        return ResolvedImageReference(key = key, url = url)
    }

    private fun extractKeyFromUrl(url: String): String? {
        return runCatching {
            URI(url).path
                ?.removePrefix("/")
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun buildPublicUrl(key: String): String {
        val base = appS3Properties.publicBaseUrl.trim().trimEnd('/')
        return if (base.isBlank()) key else "$base/$key"
    }

    data class ResolvedImageReference(
        val key: String?,
        val url: String,
    )
}
