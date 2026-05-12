package com.moongchijang.domain.search.application

import org.springframework.stereotype.Component
import kotlin.math.max
import kotlin.math.min

@Component
class ProductNormalizer(
    private val aliasDictionary: AliasDictionary
) {
    fun normalize(query: String, extractedProduct: String?, validProducts: List<String>): String? {
        if (extractedProduct in validProducts) return extractedProduct

        val aliasProduct = aliasDictionary.resolveProduct(query, validProducts)
        if (aliasProduct != null) return aliasProduct

        return fuzzyMatch(query, validProducts)
    }

    private fun fuzzyMatch(query: String, validProducts: List<String>): String? {
        val tokens = tokenize(query).filter { it.length > MIN_FUZZY_TOKEN_LENGTH }
        if (tokens.isEmpty() || validProducts.isEmpty()) return null

        val ranked = validProducts
            .map { product ->
                product to tokens.maxOf { token -> similarity(token, product) }
            }
            .sortedByDescending { it.second }

        val best = ranked.firstOrNull() ?: return null
        if (best.second < FUZZY_THRESHOLD) return null

        val secondScore = ranked.getOrNull(1)?.second ?: 0.0
        if (best.second - secondScore < FUZZY_MIN_MARGIN) return null

        return best.first
    }

    private fun tokenize(query: String): List<String> =
        TOKEN_REGEX.findAll(query)
            .map { it.value.trim() }
            .filter { it.isNotEmpty() }
            .toList()

    private fun similarity(source: String, target: String): Double =
        max(
            max(
                normalizedLevenshtein(source, target),
                normalizedLevenshtein(decomposeHangul(source), decomposeHangul(target))
            ),
            jaroWinkler(source, target),
        )

    private fun normalizedLevenshtein(source: String, target: String): Double {
        val maxLength = max(source.length, target.length)
        if (maxLength == 0) return 1.0

        return 1.0 - levenshteinDistance(source, target).toDouble() / maxLength
    }

    private fun levenshteinDistance(source: String, target: String): Int {
        if (source == target) return 0
        if (source.isEmpty()) return target.length
        if (target.isEmpty()) return source.length

        var previous = IntArray(target.length + 1) { it }
        var current = IntArray(target.length + 1)

        for (i in source.indices) {
            current[0] = i + 1
            for (j in target.indices) {
                val substitutionCost = if (source[i] == target[j]) 0 else 1
                current[j + 1] = min(
                    min(current[j] + 1, previous[j + 1] + 1),
                    previous[j] + substitutionCost
                )
            }
            val temp = previous
            previous = current
            current = temp
        }

        return previous[target.length]
    }

    private fun jaroWinkler(source: String, target: String): Double {
        if (source == target) return 1.0
        if (source.isEmpty() || target.isEmpty()) return 0.0

        val matchDistance = max(source.length, target.length) / 2 - 1
        val sourceMatches = BooleanArray(source.length)
        val targetMatches = BooleanArray(target.length)
        var matches = 0

        for (i in source.indices) {
            val start = max(0, i - matchDistance)
            val end = min(i + matchDistance + 1, target.length)
            for (j in start until end) {
                if (targetMatches[j] || source[i] != target[j]) continue
                sourceMatches[i] = true
                targetMatches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0
        var targetIndex = 0
        for (i in source.indices) {
            if (!sourceMatches[i]) continue
            while (!targetMatches[targetIndex]) targetIndex++
            if (source[i] != target[targetIndex]) transpositions++
            targetIndex++
        }

        val jaro = (
            matches.toDouble() / source.length +
                matches.toDouble() / target.length +
                (matches - transpositions / 2.0) / matches
            ) / 3.0
        val prefixLength = commonPrefixLength(source, target)

        return jaro + prefixLength * JARO_WINKLER_PREFIX_SCALE * (1.0 - jaro)
    }

    private fun commonPrefixLength(source: String, target: String): Int {
        val maxPrefix = min(min(source.length, target.length), JARO_WINKLER_MAX_PREFIX)
        var count = 0
        while (count < maxPrefix && source[count] == target[count]) {
            count++
        }
        return count
    }

    private fun decomposeHangul(value: String): String =
        buildString {
            for (char in value) {
                val syllableIndex = char.code - HANGUL_BASE
                if (syllableIndex !in 0 until HANGUL_SYLLABLE_COUNT) {
                    append(char)
                    continue
                }

                val choseongIndex = syllableIndex / (JUNGSEONG_COUNT * JONGSEONG_COUNT)
                val jungseongIndex = syllableIndex % (JUNGSEONG_COUNT * JONGSEONG_COUNT) / JONGSEONG_COUNT
                val jongseongIndex = syllableIndex % JONGSEONG_COUNT

                append(CHOSEONG[choseongIndex])
                append(JUNGSEONG[jungseongIndex])
                if (jongseongIndex > 0) append(JONGSEONG[jongseongIndex])
            }
        }

    companion object {
        private val TOKEN_REGEX = Regex("[\\p{L}\\p{N}]+")
        private const val MIN_FUZZY_TOKEN_LENGTH = 1
        private const val FUZZY_THRESHOLD = 0.76
        private const val FUZZY_MIN_MARGIN = 0.08
        private const val JARO_WINKLER_PREFIX_SCALE = 0.1
        private const val JARO_WINKLER_MAX_PREFIX = 4
        private const val HANGUL_BASE = 0xAC00
        private const val HANGUL_SYLLABLE_COUNT = 11172
        private const val JUNGSEONG_COUNT = 21
        private const val JONGSEONG_COUNT = 28
        private val CHOSEONG = charArrayOf(
            'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
        )
        private val JUNGSEONG = charArrayOf(
            'ㅏ', 'ㅐ', 'ㅑ', 'ㅒ', 'ㅓ', 'ㅔ', 'ㅕ', 'ㅖ', 'ㅗ', 'ㅘ',
            'ㅙ', 'ㅚ', 'ㅛ', 'ㅜ', 'ㅝ', 'ㅞ', 'ㅟ', 'ㅠ', 'ㅡ', 'ㅢ', 'ㅣ'
        )
        private val JONGSEONG = charArrayOf(
            '\u0000', 'ㄱ', 'ㄲ', 'ㄳ', 'ㄴ', 'ㄵ', 'ㄶ', 'ㄷ', 'ㄹ', 'ㄺ',
            'ㄻ', 'ㄼ', 'ㄽ', 'ㄾ', 'ㄿ', 'ㅀ', 'ㅁ', 'ㅂ', 'ㅄ', 'ㅅ',
            'ㅆ', 'ㅇ', 'ㅈ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
        )
    }
}
