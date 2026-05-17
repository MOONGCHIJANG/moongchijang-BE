package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyViewerCountResponse
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyImage
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyViewerCountRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GroupBuyViewerService(
    private val groupBuyRepository: GroupBuyRepository,
    private val groupBuyViewerCountRepository: GroupBuyViewerCountRepository,
) {
    companion object {
        private const val ACTIVE_VIEWER_TTL_SECONDS = 90L
        private const val FOMO_THRESHOLD = 10
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

    @Transactional
    fun heartbeat(groupBuyId: Long, userId: Long?, viewerSessionId: String): GroupBuyViewerCountResponse {
        log.info(
            "[GroupBuyViewerService] heartbeat 처리 시작: groupBuyId={}, userId={}, viewerSessionId={}",
            groupBuyId, userId, viewerSessionId
        )
        ensureGroupBuyExists(groupBuyId)

        val now = Instant.now().epochSecond
        val viewerKey = if (userId != null) "user:$userId" else "session:$viewerSessionId"

        groupBuyViewerCountRepository.touch(
            groupBuyId = groupBuyId,
            viewerKey = viewerKey,
            nowEpochSeconds = now,
            ttlSeconds = ACTIVE_VIEWER_TTL_SECONDS
        )

        val count = groupBuyViewerCountRepository
            .countActive(groupBuyId, now, ACTIVE_VIEWER_TTL_SECONDS)
            .toInt()

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
        if (!groupBuyRepository.existsById(groupBuyId)) {
            log.warn("[GroupBuyViewerService] 공구 없음: groupBuyId={}", groupBuyId)
            throw CustomException(ErrorCode.GROUPBUY_NOT_FOUND)
        }
    }
}
