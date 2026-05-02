package com.moongchijang.domain.region.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "관심지역 선택 트리 응답")
data class RegionTreeResponse(

    @field:Schema(description = "시/도 노드 목록")
    val regions: List<RegionNodeDto>
)
