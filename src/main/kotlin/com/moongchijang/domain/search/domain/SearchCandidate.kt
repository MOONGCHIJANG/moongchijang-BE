package com.moongchijang.domain.search.domain

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy

data class SearchCandidate(
    val groupBuy: GroupBuy,
    val exactScore: Double = 0.0,
    val aliasScore: Double = 0.0,
    val vectorScore: Double = 0.0
) {
    val finalScore: Double =
        exactScore * 0.45 + aliasScore * 0.2 + vectorScore * 0.35

    val matchedBy: Set<String> =
        buildSet {
            if (exactScore > 0.0) add("EXACT")
            if (aliasScore > 0.0) add("ALIAS")
            if (vectorScore > 0.0) add("VECTOR")
        }
}
