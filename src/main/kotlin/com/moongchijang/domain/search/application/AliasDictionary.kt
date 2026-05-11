package com.moongchijang.domain.search.application

import org.springframework.stereotype.Component

@Component
class AliasDictionary {
    private val productAliases = mapOf(
        "시오빵" to "소금빵",
        "salt bread" to "소금빵",
        "소금 빵" to "소금빵",
        "버터 떡" to "버터떡"
    )

    fun resolveProduct(query: String, validProducts: List<String>): String? {
        val direct = validProducts.firstOrNull { query.contains(it, ignoreCase = true) }
        if (direct != null) return direct

        return productAliases.entries
            .firstOrNull { (alias, product) ->
                query.contains(alias, ignoreCase = true) && product in validProducts
            }
            ?.value
    }

    fun isAliasMatch(query: String, product: String?): Boolean {
        if (product == null) return false
        return productAliases.any { (alias, canonical) ->
            canonical == product && query.contains(alias, ignoreCase = true)
        }
    }
}
