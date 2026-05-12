package com.moongchijang.domain.search.infrastructure.demo

import java.util.Locale

object LocalDemoSearchVocabulary {
    private val productAliases = linkedMapOf(
        "시오빵" to "소금빵",
        "소금 빵" to "소금빵",
        "salt bread" to "소금빵",
        "크로와상" to "크루아상",
        "croissant" to "크루아상",
        "버터 떡" to "버터떡",
        "마들랜" to "마들렌",
        "madeleine" to "마들렌"
    )

    fun canonicalize(text: String): String {
        var normalized = text.lowercase(Locale.getDefault())
            .replace(Regex("\\s+"), " ")
            .trim()

        productAliases.forEach { (alias, canonical) ->
            normalized = normalized.replace(alias.lowercase(Locale.getDefault()), canonical)
            normalized = normalized.replace(alias.replace(" ", "").lowercase(Locale.getDefault()), canonical)
        }

        return normalized.replace(Regex("\\s+"), " ").trim()
    }

    fun embeddingText(text: String): String =
        canonicalize(text).replace(" ", "")

    fun detectRegion(query: String, validRegions: List<String>): String? {
        val queryText = embeddingText(query)
        return validRegions.firstOrNull { region ->
            val canonicalRegion = embeddingText(region)
            queryText.contains(canonicalRegion)
        }
    }

    fun detectProduct(query: String, validProducts: List<String>): String? {
        val normalizedQuery = canonicalize(query)
        val compactQuery = embeddingText(query)

        validProducts.firstOrNull { product ->
            val canonicalProduct = embeddingText(product)
            normalizedQuery.contains(canonicalProduct) || compactQuery.contains(canonicalProduct)
        }?.let { return it }

        return productAliases.entries.firstOrNull { (alias, canonical) ->
            val aliasCanonical = embeddingText(alias)
            compactQuery.contains(aliasCanonical) && validProducts.any { it == canonical }
        }?.value
    }

    fun detectFromPrompt(prompt: String): Extraction {
        val query = extractQuery(prompt)
        val validRegions = extractList(prompt, "유효 동네")
        val validProducts = extractList(prompt, "유효 상품")

        return Extraction(
            query = query,
            region = detectRegion(query, validRegions),
            product = detectProduct(query, validProducts)
        )
    }

    private fun extractQuery(prompt: String): String {
        val match = Regex("""사용자 검색어:\s*"([^"]*)"""").find(prompt)
        return match?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun extractList(prompt: String, label: String): List<String> {
        val match = Regex("""^\s*$label\s*:\s*(.*)$""", RegexOption.MULTILINE).find(prompt)
        val raw = match?.groupValues?.getOrNull(1).orEmpty()
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    data class Extraction(
        val query: String,
        val region: String?,
        val product: String?
    )
}
