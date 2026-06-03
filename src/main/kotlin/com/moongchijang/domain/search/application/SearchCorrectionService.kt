package com.moongchijang.domain.search.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.search.domain.repository.SearchCorrectionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchCorrectionService(
    private val searchCorrectionRepository: SearchCorrectionRepository,
    private val groupBuyRepository: GroupBuyRepository,
) {
    @Transactional(readOnly = true)
    fun correct(query: String): String? {
        val normalized = SearchQueryNormalizer.normalize(query)
        if (normalized.isBlank()) {
            return null
        }

        searchCorrectionRepository.findBySourceKeywordAndEnabledTrue(normalized)?.let {
            return it.targetKeyword
        }

        return findSimilarCorrection(normalized)
    }

    private fun findSimilarCorrection(normalized: String): String? {
        if (normalized.length < MIN_SIMILARITY_QUERY_LENGTH) {
            return null
        }

        return correctionCandidates(normalized.length)
            .asSequence()
            .filter { it != normalized }
            .mapNotNull { candidate ->
                val distance = jamoEditDistance(normalized, candidate)
                candidate.takeIf { distance <= maxDistance(normalized.length) }
                    ?.let { SimilarCandidate(candidate = it, distance = distance) }
            }
            .minWithOrNull(
                compareBy<SimilarCandidate> { it.distance }
                    .thenBy { kotlin.math.abs(it.candidate.length - normalized.length) }
                    .thenBy { it.candidate.length }
            )
            ?.candidate
    }

    private fun correctionCandidates(queryLength: Int): List<String> {
        val dictionaryTargets = searchCorrectionRepository.findAllByEnabledTrue()
            .map { SearchQueryNormalizer.normalize(it.targetKeyword) }

        val activeFeedTerms = groupBuyRepository.findActiveSearchKeywords(GroupBuyStatus.IN_PROGRESS.name)
            .flatMap { extractTerms(SearchQueryNormalizer.normalize(it), queryLength) }

        return (dictionaryTargets + activeFeedTerms)
            .filter { it.length >= MIN_SIMILARITY_QUERY_LENGTH }
            .distinct()
    }

    private fun extractTerms(productName: String, queryLength: Int): List<String> {
        if (productName.isBlank()) {
            return emptyList()
        }

        val windowSizes = ((queryLength - 1)..(queryLength + 1))
            .filter { it >= MIN_SIMILARITY_QUERY_LENGTH }
            .filter { it <= productName.length }

        return buildList {
            if (productName.length <= queryLength + 1) {
                add(productName)
            }
            windowSizes.forEach { size ->
                for (start in 0..(productName.length - size)) {
                    add(productName.substring(start, start + size))
                }
            }
        }
    }

    private fun maxDistance(queryLength: Int): Int = when {
        queryLength <= 3 -> 1
        queryLength <= 5 -> 2
        else -> 3
    }

    private fun jamoEditDistance(source: String, target: String): Int {
        val left = decomposeHangul(source)
        val right = decomposeHangul(target)
        val dp = Array(left.size + 1) { IntArray(right.size + 1) }

        for (i in 0..left.size) {
            dp[i][0] = i
        }
        for (j in 0..right.size) {
            dp[0][j] = j
        }

        for (i in 1..left.size) {
            for (j in 1..right.size) {
                val cost = if (left[i - 1] == right[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[left.size][right.size]
    }

    private fun decomposeHangul(value: String): List<Int> = buildList {
        value.forEach { char ->
            val syllableIndex = char.code - HANGUL_BASE
            if (syllableIndex in 0 until HANGUL_SYLLABLE_COUNT) {
                add(syllableIndex / (JUNG_COUNT * JONG_COUNT))
                add(CHO_COUNT + (syllableIndex % (JUNG_COUNT * JONG_COUNT)) / JONG_COUNT)
                val jong = syllableIndex % JONG_COUNT
                if (jong > 0) {
                    add(CHO_COUNT + JUNG_COUNT + jong)
                }
            } else {
                add(NON_HANGUL_OFFSET + char.code)
            }
        }
    }

    private data class SimilarCandidate(
        val candidate: String,
        val distance: Int,
    )

    companion object {
        private const val MIN_SIMILARITY_QUERY_LENGTH = 2
        private const val HANGUL_BASE = 0xAC00
        private const val HANGUL_SYLLABLE_COUNT = 11172
        private const val CHO_COUNT = 19
        private const val JUNG_COUNT = 21
        private const val JONG_COUNT = 28
        private const val NON_HANGUL_OFFSET = 10_000
    }
}
