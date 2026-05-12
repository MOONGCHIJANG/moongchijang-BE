package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.search.application.port.VectorSearchCandidate
import com.moongchijang.domain.search.domain.SearchIntent
import com.moongchijang.global.config.SearchProperties
import org.springframework.stereotype.Component

@Component
class VectorCandidatePromotionGuard(
    private val searchProperties: SearchProperties = SearchProperties()
) {
    fun filterPromotableCandidates(
        intent: SearchIntent,
        vectorMatches: List<GroupBuy>,
        vectorCandidates: List<VectorSearchCandidate>
    ): VectorPromotionResult {
        if (!searchProperties.guard.enabled) {
            return VectorPromotionResult(
                vectorMatches = vectorMatches,
                vectorCandidates = vectorCandidates,
                decisions = vectorCandidates.map {
                    VectorCandidatePromotionDecision(
                        groupBuyId = it.groupBuyId,
                        score = it.score,
                        rejectionReason = null
                    )
                }
            )
        }

        val scoresById = vectorCandidates.associate { it.groupBuyId to it.score }
        val vectorMatchesById = vectorMatches.associateBy { it.id }
        val hasKnownToken = intent.region != null || intent.product != null
        val hasEnoughConfidence = intent.confidence >= searchProperties.guard.minConfidence

        val decisions = vectorCandidates.map { vectorCandidate ->
            val groupBuy = vectorMatchesById[vectorCandidate.groupBuyId]
            val rejectionReason = when {
                groupBuy == null -> VectorCandidateRejectionReason.UNAVAILABLE_METADATA
                !hasKnownToken -> VectorCandidateRejectionReason.NO_KNOWN_TOKEN
                !hasEnoughConfidence -> VectorCandidateRejectionReason.LOW_CONFIDENCE
                !metadataCompatible(intent, groupBuy) -> VectorCandidateRejectionReason.METADATA_MISMATCH
                vectorCandidate.score < supportiveScoreFloor(intent) -> VectorCandidateRejectionReason.LOW_SCORE
                else -> null
            }
            VectorCandidatePromotionDecision(
                groupBuyId = vectorCandidate.groupBuyId,
                score = vectorCandidate.score,
                rejectionReason = rejectionReason
            )
        }
        val promotableIds = decisions
            .filter { it.rejectionReason == null }
            .map { it.groupBuyId }
            .toSet()

        return VectorPromotionResult(
            vectorMatches = vectorMatches.filter { it.id in promotableIds },
            vectorCandidates = vectorCandidates.filter { it.groupBuyId in promotableIds },
            decisions = decisions
        )
    }

    private fun supportiveScoreFloor(intent: SearchIntent): Double =
        if (intent.region == null && intent.product == null) {
            searchProperties.guard.supportiveScoreThreshold
        } else {
            0.0
        }

    private fun metadataCompatible(intent: SearchIntent, groupBuy: GroupBuy): Boolean {
        val regionMatches = intent.region == null || groupBuy.store.region.label == intent.region
        val productMatches = intent.product == null || groupBuy.productName == intent.product
        return regionMatches && productMatches
    }
}

data class VectorPromotionResult(
    val vectorMatches: List<GroupBuy>,
    val vectorCandidates: List<VectorSearchCandidate>,
    val decisions: List<VectorCandidatePromotionDecision>
) {
    val rejectedCount: Int =
        decisions.count { it.rejectionReason != null }

    val rejectionReasonCounts: Map<VectorCandidateRejectionReason, Int> =
        decisions
            .mapNotNull { it.rejectionReason }
            .groupingBy { it }
            .eachCount()
}

data class VectorCandidatePromotionDecision(
    val groupBuyId: Long,
    val score: Double,
    val rejectionReason: VectorCandidateRejectionReason?
)

enum class VectorCandidateRejectionReason {
    NO_KNOWN_TOKEN,
    LOW_CONFIDENCE,
    METADATA_MISMATCH,
    LOW_SCORE,
    UNAVAILABLE_METADATA
}
