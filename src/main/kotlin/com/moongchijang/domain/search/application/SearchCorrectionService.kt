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

        val maxDistance = maxDistance(normalized.length)
        return correctionCandidates(normalized.length)
            .asSequence()
            .filter { it != normalized }
            .mapNotNull { candidate ->
                val distance = jamoEditDistance(normalized, candidate, maxDistance)
                    ?: return@mapNotNull null
                SimilarCandidate(candidate = candidate, distance = distance)
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

        val activeFeedTerms = groupBuyRepository.findActiveSearchKeywords(
            status = GroupBuyStatus.IN_PROGRESS.name,
            limit = ACTIVE_KEYWORD_LIMIT,
        )
            .flatMap { extractTerms(SearchQueryNormalizer.normalize(it), queryLength) }

        return (dictionaryTargets + activeFeedTerms)
            .filter { it.length >= MIN_SIMILARITY_QUERY_LENGTH }
            .distinct()
            .take(MAX_SIMILARITY_CANDIDATES)
    }

    private fun extractTerms(productName: String, queryLength: Int): List<String> {
        if (productName.isBlank()) {
            return emptyList()
        }

        val windowSizes = ((queryLength - 1)..(queryLength + 1))
            .filter { it >= MIN_SIMILARITY_QUERY_LENGTH }
            .filter { it <= productName.length }

        return buildSet {
            if (productName.length <= queryLength + 1) {
                add(productName)
            }
            windowSizes.forEach { size ->
                for (start in 0..(productName.length - size)) {
                    add(productName.substring(start, start + size))
                }
            }
        }.toList()
    }

    private fun maxDistance(queryLength: Int): Int = when {
        queryLength <= 3 -> 1
        queryLength <= 5 -> 2
        else -> 3
    }

    private fun jamoEditDistance(source: String, target: String, maxDistance: Int): Int? {
        val left = decomposeHangul(source)
        val right = decomposeHangul(target)
        if (kotlin.math.abs(left.size - right.size) > maxDistance) {
            return null
        }

        var previous = IntArray(right.size + 1) { it }
        var current = IntArray(right.size + 1)
        for (j in 0..right.size) {
            previous[j] = j
        }

        for (i in 1..left.size) {
            current[0] = i
            var rowMin = current[0]
            for (j in 1..right.size) {
                val cost = if (left[i - 1] == right[j - 1]) 0 else 1
                current[j] = minOf(
                    previous[j] + 1,
                    current[j - 1] + 1,
                    previous[j - 1] + cost
                )
                rowMin = minOf(rowMin, current[j])
            }
            if (rowMin > maxDistance) {
                return null
            }
            val swap = previous
            previous = current
            current = swap
        }

        return previous[right.size].takeIf { it <= maxDistance }
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
        private const val ACTIVE_KEYWORD_LIMIT = 500
        private const val MAX_SIMILARITY_CANDIDATES = 2_000
        private const val HANGUL_BASE = 0xAC00
        private const val HANGUL_SYLLABLE_COUNT = 11172
        private const val CHO_COUNT = 19
        private const val JUNG_COUNT = 21
        private const val JONG_COUNT = 28
        private const val NON_HANGUL_OFFSET = 10_000
    }
}
