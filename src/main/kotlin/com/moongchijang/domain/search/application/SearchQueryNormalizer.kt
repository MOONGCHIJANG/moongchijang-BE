package com.moongchijang.domain.search.application

object SearchQueryNormalizer {
    private val SEPARATORS = Regex("""[\s\-_]+""")
    private val SPECIAL_CHARS = Regex("""[^\p{L}\p{N}]""")

    fun normalize(query: String): String =
        query.trim()
            .replace(SEPARATORS, "")
            .replace(SPECIAL_CHARS, "")
            .lowercase()
}
