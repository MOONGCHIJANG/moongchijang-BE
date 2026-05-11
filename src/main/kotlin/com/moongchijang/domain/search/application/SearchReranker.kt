package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.search.application.port.VectorSearchCandidate
import com.moongchijang.domain.search.domain.SearchCandidate
import org.springframework.stereotype.Component

@Component
class SearchReranker {
    fun merge(
        exactMatches: List<GroupBuy>,
        vectorMatches: List<GroupBuy>,
        vectorCandidates: List<VectorSearchCandidate>,
        aliasMatched: Boolean
    ): List<SearchCandidate> {
        val exactIds = exactMatches.map { it.id }.toSet()
        val vectorScores = vectorCandidates.associate { it.groupBuyId to it.score }
        val groupBuys = (exactMatches + vectorMatches).associateBy { it.id }.values

        return groupBuys
            .map { groupBuy ->
                SearchCandidate(
                    groupBuy = groupBuy,
                    exactScore = if (groupBuy.id in exactIds) 1.0 else 0.0,
                    aliasScore = if (aliasMatched && groupBuy.id in exactIds) 1.0 else 0.0,
                    vectorScore = vectorScores[groupBuy.id] ?: 0.0
                )
            }
            .sortedByDescending { it.finalScore }
    }
}
