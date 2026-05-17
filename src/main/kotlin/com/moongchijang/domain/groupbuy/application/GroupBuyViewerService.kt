package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyViewerCountResponse
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyViewerCountRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.Duration

@Service
class GroupBuyViewerService(
    private val groupBuyRepository: GroupBuyRepository,
    private val groupBuyViewerCountRepository: GroupBuyViewerCountRepository,
    private val redisTemplate: StringRedisTemplate,
) {
    companion object {
        private const val ACTIVE_VIEWER_TTL_SECONDS = 90L
        private const val FOMO_THRESHOLD = 10
        private const val EXISTS_CACHE_TTL_SECONDS = 120L
        private const val EXISTS_CACHE_TRUE = "1"
        private const val EXISTS_CACHE_FALSE = "0"
    }

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getActiveViewerCount(groupBuyId: Long): GroupBuyViewerCountResponse {
        log.info("[GroupBuyViewerService] 활성 조회자 수 조회 시작: groupBuyId={}", groupBuyId)
        ensureGroupBuyExists(groupBuyId)

        val now = Instant.now().epochSecond
        val count = groupBuyViewerCountRepository
            .countActive(groupBuyId, now, ACTIVE_VIEWER_TTL_SECONDS)
            .toInt()

        val response = GroupBuyViewerCountResponse(
            activeViewerCount = count,
            showFomoBadge = count >= FOMO_THRESHOLD,
            threshold = FOMO_THRESHOLD
        )

        log.info(
            "[GroupBuyViewerService] 활성 조회자 수 조회 완료: groupBuyId={}, activeViewerCount={}, showFomoBadge={}",
            groupBuyId, response.activeViewerCount, response.showFomoBadge
        )
        return response
    }

    fun heartbeat(groupBuyId: Long, userId: Long?, viewerSessionId: String): GroupBuyViewerCountResponse {
        log.info(
            "[GroupBuyViewerService] heartbeat 처리 시작: groupBuyId={}, userId={}, viewerSessionId={}",
            groupBuyId, userId, viewerSessionId
        )
        ensureGroupBuyExists(groupBuyId)

        val now = Instant.now().epochSecond
        val viewerKey = if (userId != null) "user:$userId" else "session:$viewerSessionId"

        val count = groupBuyViewerCountRepository.touchAndCount(
            groupBuyId = groupBuyId,
            viewerKey = viewerKey,
            nowEpochSeconds = now,
            ttlSeconds = ACTIVE_VIEWER_TTL_SECONDS
        ).toInt()

        val response = GroupBuyViewerCountResponse(
            activeViewerCount = count,
            showFomoBadge = count >= FOMO_THRESHOLD,
            threshold = FOMO_THRESHOLD
        )

        log.info(
            "[GroupBuyViewerService] heartbeat 처리 완료: groupBuyId={}, viewerKey={}, activeViewerCount={}, showFomoBadge={}",
            groupBuyId, viewerKey, response.activeViewerCount, response.showFomoBadge
        )
        return response
    }

    private fun ensureGroupBuyExists(groupBuyId: Long) {
        val cacheKey = existsCacheKey(groupBuyId)
        val cached = redisTemplate.opsForValue().get(cacheKey)

        if (cached == EXISTS_CACHE_TRUE) return
        if (cached == EXISTS_CACHE_FALSE) {
            log.warn("[GroupBuyViewerService] 공구 없음: groupBuyId={}", groupBuyId)
            throw CustomException(ErrorCode.GROUPBUY_NOT_FOUND)
        }

        val exists = groupBuyRepository.existsById(groupBuyId)
        redisTemplate.opsForValue().set(
            cacheKey,
            if (exists) EXISTS_CACHE_TRUE else EXISTS_CACHE_FALSE,
            Duration.ofSeconds(EXISTS_CACHE_TTL_SECONDS)
        )

        if (!exists) {
            log.warn("[GroupBuyViewerService] 공구 없음: groupBuyId={}", groupBuyId)
            throw CustomException(ErrorCode.GROUPBUY_NOT_FOUND)
        }
    }

    private fun existsCacheKey(groupBuyId: Long): String = "groupBuy:exists:$groupBuyId"
}
