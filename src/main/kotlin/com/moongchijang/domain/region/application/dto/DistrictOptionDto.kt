package com.moongchijang.domain.region.application.dto

import com.moongchijang.domain.store.domain.entity.DistrictType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "세부지역 옵션")
data class DistrictOptionDto(

    @field:Schema(
        description = "세부지역 타입 코드",
        example = "SEOUL_GANGNAM_YEOKSAM_SAMSEONG"
    )
    val type: DistrictType,

    @field:Schema(
        description = "세부지역 한글 라벨",
        example = "강남 | 역삼 | 삼성"
    )
    val label: String
)
