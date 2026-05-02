package com.moongchijang.domain.region.application.dto

import com.moongchijang.domain.store.domain.entity.RegionType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "시/도 노드")
data class RegionNodeDto(

    @field:Schema(
        description = "시/도 타입 코드",
        example = "SEOUL"
    )
    val regionType: RegionType,

    @field:Schema(
        description = "시/도 한글 라벨",
        example = "서울"
    )
    val regionLabel: String,

    @field:Schema(description = "해당 시/도 전체 선택 옵션")
    val allDistrict: DistrictOptionDto,

    @field:Schema(description = "해당 시/도 하위 세부지역 목록 (*_ALL 제외)")
    val districts: List<DistrictOptionDto>
)
