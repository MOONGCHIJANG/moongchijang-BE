package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyDetailResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedItemResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedPageResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedRequest
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyProgressItem
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyProgressCalculator
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyProgressResponse
import com.moongchijang.domain.groupbuy.application.dto.ShareMetaResponse
import com.moongchijang.domain.groupbuy.domain.repository.FeedSortMode
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyImageRepository
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.time.kstNow
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

@Service
class GroupBuyService(
    private val groupBuyRepository: GroupBuyRepository,
    private val groupBuyImageRepository: GroupBuyImageRepository,
    private val favoriteRepository: FavoriteRepository,
    private val participationRepository: ParticipationRepository,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
    private val clock: Clock,
    @Value("\${app.share.base-url}") private val shareBaseUrl: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getFeed(request: GroupBuyFeedRequest, pageable: Pageable): GroupBuyFeedPageResponse {
        log.info(
            "[GroupBuyService] 공구 피드 조회 시작: filter={}, districts={}, page={}, size={}",
            request.filter, request.districts, pageable.pageNumber, pageable.pageSize
        )

        validateDistrictSelection(request.districts)

        val expandedDistricts = expandAllDistricts(request.districts)

        var hasRegionalResult = true

        var resultPage = groupBuyRepository.searchFeed(
            filter = request.filter,
            districtFilters = expandedDistricts,
            pageable = pageable,
            sortMode = FeedSortMode.REGIONAL
        )

        if (expandedDistricts.isNotEmpty() && resultPage.totalElements == 0L) {
            hasRegionalResult = false
            resultPage = groupBuyRepository.searchFeed(
                filter = request.filter,
                districtFilters = emptySet(), // 전국 fallback
                pageable = pageable,
                sortMode = FeedSortMode.NATIONWIDE_FALLBACK
            )
        }

        log.info(
            "[GroupBuyService] 공구 피드 조회 완료: totalElements={}, totalPages={}, currentPage={}",
            resultPage.totalElements, resultPage.totalPages, resultPage.number + 1
        )

        val now = clock.kstNow()
        return GroupBuyFeedPageResponse.from(
            resultPage.map {
                GroupBuyFeedItemResponse.from(
                    groupBuy = it,
                    now = now,
                    thumbnailUrl = s3ImageReferenceResolver.resolveForRead(it.thumbnailKey),
                )
            },
            hasRegionalResult = hasRegionalResult
        )
    }

    @Transactional(readOnly = true)
    fun getDetail(groupBuyId: Long, userId: Long?): GroupBuyDetailResponse {
        log.info("[GroupBuyService] 공구 상세 조회 시작: groupBuyId={}, userId={}", groupBuyId, userId)

        val groupBuy = groupBuyRepository.findWithStoreById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

        val images = groupBuyImageRepository.findAllByGroupBuyId(groupBuyId)

        val isWishlisted = userId?.let {
            favoriteRepository.existsByUserIdAndGroupBuyId(it, groupBuyId)
        } ?: false

        val isParticipated = userId?.let {
            participationRepository.existsByUserIdAndGroupBuyId(it, groupBuyId)
        } ?: false

        val isClosed = GroupBuyProgressCalculator.isClosed(groupBuy)
        val now = clock.kstNow()
        val canParticipate = !isParticipated &&
            !isClosed &&
            groupBuy.deadline.isAfter(now) &&
            groupBuy.currentQuantity < groupBuy.maxQuantity

        log.info(
            "[GroupBuyService] 공구 상세 조회 완료: groupBuyId={}, isWishlisted={}, isParticipated={}, canParticipate={}",
            groupBuyId, isWishlisted, isParticipated, canParticipate
        )

        return GroupBuyDetailResponse.from(
            groupBuy = groupBuy,
            thumbnailUrl = s3ImageReferenceResolver.resolveForRead(groupBuy.thumbnailKey),
            imageUrls = images
                .filter { it.imageKey != groupBuy.thumbnailKey }
                .mapNotNull { s3ImageReferenceResolver.resolveForRead(it.imageKey) },
            isWishlisted = isWishlisted,
            isParticipated = isParticipated,
            canParticipate = canParticipate,
            now = now,
        )
    }

    @Transactional(readOnly = true)
    fun getShareMeta(groupBuyId: Long): ShareMetaResponse {
        log.debug("[GroupBuyService] 공구 공유 메타데이터 조회 시작: groupBuyId={}", groupBuyId)

        val groupBuy = groupBuyRepository.findWithStoreById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

        val response = ShareMetaResponse.from(
            groupBuy = groupBuy,
            shareUrl = buildShareUrl(groupBuy.id),
            imageUrl = s3ImageReferenceResolver.resolveForRead(groupBuy.thumbnailKey),
        )

        log.debug("[GroupBuyService] 공구 공유 메타데이터 조회 완료: groupBuyId={}", groupBuyId)
        return response
    }

    @Transactional(readOnly = true, timeout = 3)
    fun getProgress(groupBuyId: Long): GroupBuyProgressResponse {
        log.debug("[GroupBuyService] 공구 progress 단건 조회 시작: groupBuyId={}", groupBuyId)

        val groupBuy = groupBuyRepository.findById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

        val response = GroupBuyProgressResponse.from(groupBuy)
        log.debug(
            "[GroupBuyService] 공구 progress 단건 조회 완료: groupBuyId={}, currentQuantity={}, targetQuantity={}, achievementRate={}, isClosed={}",
            response.groupBuyId,
            response.currentQuantity,
            response.targetQuantity,
            response.achievementRate,
            response.isClosed
        )
        return response
    }

    @Transactional(readOnly = true, timeout = 3)
    fun getProgresses(groupBuyIds: List<Long>): List<GroupBuyProgressItem> {
        log.debug("[GroupBuyService] 공구 progress 다건 조회 시작: requestedSize={}", groupBuyIds.size)

        if (groupBuyIds.isEmpty()) {
            log.debug("[GroupBuyService] 공구 progress 다건 조회 완료: requestedSize=0, returnedSize=0")
            return emptyList()
        }

        val now = clock.kstNow()
        val groupBuysById = groupBuyRepository.findAllById(groupBuyIds).associateBy { it.id }

        val response = groupBuyIds.mapNotNull { id ->
            groupBuysById[id]?.let { GroupBuyProgressItem.from(it, now) }
        }
        log.debug(
            "[GroupBuyService] 공구 progress 다건 조회 완료: requestedSize={}, returnedSize={}",
            groupBuyIds.size,
            response.size
        )
        return response
    }

    private fun validateDistrictSelection(districts: List<DistrictType>) {
        if (districts.size > 10) {
            throw CustomException(ErrorCode.GROUPBUY_FEED_TOO_MANY_DISTRICTS)
        }

        districts.groupBy { it.region }.forEach { (_, values) ->
            val hasAll = values.any { it.name.endsWith("_ALL") }
            val hasChild = values.any { !it.name.endsWith("_ALL") }

            if (hasAll && hasChild) {
                throw CustomException(ErrorCode.GROUPBUY_FEED_INVALID_DISTRICT_COMBINATION)
            }
        }
    }

    private fun expandAllDistricts(districts: List<DistrictType>): Set<DistrictType> {
        return districts.flatMap { district ->
            if (district.name.endsWith("_ALL")) {
                DistrictType.findLeafByRegion(district.region)
            } else {
                listOf(district)
            }
        }.toSet()
    }

    private fun buildShareUrl(groupBuyId: Long): String {
        return "${shareBaseUrl.trimEnd('/')}/group-buys/$groupBuyId"
    }
}
