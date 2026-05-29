package com.moongchijang.domain.favorite.application

import com.moongchijang.domain.favorite.application.dto.WishFilterType
import com.moongchijang.domain.favorite.application.dto.WishSortType
import com.moongchijang.domain.favorite.application.dto.WishlistPageResponse
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WishlistQueryService(
    private val favoriteRepository: FavoriteRepository,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getWishlist(
        userId: Long,
        filter: WishFilterType,
        excludeClosed: Boolean,
        sort: WishSortType,
        pageable: Pageable,
        now: LocalDateTime = LocalDateTime.now(),
    ): WishlistPageResponse {
        log.info(
            "[WishlistQueryService] 찜 목록 조회 시작: userId={}, filter={}, excludeClosed={}, sort={}, page={}, size={}",
            userId, filter, excludeClosed, sort, pageable.pageNumber, pageable.pageSize
        )

        val page = favoriteRepository.findWishlistGroupBuys(userId, filter, excludeClosed, sort, pageable, now)
        val urgentCount = favoriteRepository.countUrgentByUserId(
            userId = userId,
            now = now,
            deadlineTo = now.plusHours(24),
        ).toInt()
        val response = WishlistPageResponse.from(
            page = page,
            thumbnailUrlResolver = { s3ImageReferenceResolver.resolveForRead(it.thumbnailKey) },
            now = now,
            urgentCount = urgentCount,
        )

        log.info(
            "[WishlistQueryService] 찜 목록 조회 완료: userId={}, totalElements={}, totalPages={}, page={}, size={}, urgentCount={}",
            userId, response.totalElements, response.totalPages, response.number, response.size, response.urgentCount
        )

        return response
    }
}
