package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.store.domain.entity.DistrictType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공구 목록 조회 요청 파라미터")
data class GroupBuyFeedRequest(

    @field:Schema(description = "전체/마감임박/달성임박 단일 선택", example = "ALL")
    val filter: GroupBuyFeedFilter = GroupBuyFeedFilter.ALL,

    @field:Schema(description = "지역/세부지역 통합 필터 코드 목록 (최대 10개)")
    val districts: List<DistrictType> = emptyList(),
)
