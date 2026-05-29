package com.moongchijang.domain.image.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이미지 삭제 결과")
data class ImageDeleteResponse(
    @field:Schema(description = "삭제 완료 key 목록")
    val deletedKeys: List<String>,
    @field:Schema(description = "삭제 실패 key 목록")
    val failedKeys: List<String>,
)
