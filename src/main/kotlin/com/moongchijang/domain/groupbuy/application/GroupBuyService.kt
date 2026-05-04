package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedItemResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedPageResponse
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyFeedRequest
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroupBuyService(
    private val groupBuyRepository: GroupBuyRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getFeed(request: GroupBuyFeedRequest, pageable: Pageable):  GroupBuyFeedPageResponse {
        log.info(
            "[GroupBuyService] 공구 피드 조회 시작: filter={}, districts={}, keyword={}, page={}, size={}",
            request.filter, request.districts, request.keyword, pageable.pageNumber, pageable.pageSize
        )

        validateDistrictSelection(request.districts)

        val expandedDistricts = expandAllDistricts(request.districts)
        val keyword = request.keyword?.trim()?.takeIf { it.isNotBlank() }

        val resultPage = groupBuyRepository.searchFeed(
            filter = request.filter,
            districtFilters = expandedDistricts,
            keyword = keyword,
            pageable = pageable
        )

        log.info(
            "[GroupBuyService] 공구 피드 조회 완료: totalElements={}, totalPages={}, currentPage={}",
            resultPage.totalElements, resultPage.totalPages, resultPage.number + 1
        )

        return GroupBuyFeedPageResponse.from(
            resultPage.map { GroupBuyFeedItemResponse.from(it) }
        )
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
                DistrictType.entries.filter {
                    it.region == district.region && !it.name.endsWith("_ALL")
                }
            } else {
                listOf(district)
            }
        }.toSet()
    }
}
