package com.moongchijang.domain.image.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "이미지 업로드 카테고리")
enum class ImageUploadCategory {
    @Schema(description = "대표 썸네일 이미지")
    THUMBNAIL,

    @Schema(description = "상품 상세 이미지")
    PRODUCT,
}
