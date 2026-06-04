package com.moongchijang.domain.search.application

object SearchQueryNormalizer {
    private val SPECIAL_CHARS = Regex("""[^\p{L}\p{N}]""")

    fun normalize(query: String): String =
        query.trim()
            .replace(SPECIAL_CHARS, "")
            .lowercase()
}
