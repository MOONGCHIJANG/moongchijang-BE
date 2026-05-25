package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사업자등록번호 조회 응답")
data class BusinessRegistrationLookupResponse(

    @field:Schema(description = "사업자등록번호", example = "111-22-33333")
    val businessRegistrationNumber: String,

    @field:Schema(description = "조회 상태", example = "VALID")
    val status: BusinessRegistrationStatus,

    @field:Schema(description = "조회 결과 안내 문구", example = "사업자등록번호가 조회되지 않아 다시 입력해주세요.")
    val message: String? = null,

    @field:Schema(description = "가게명", example = "뭉치장베이커리")
    val storeName: String? = null,

    @field:Schema(description = "대표자명", example = "홍길동")
    val ownerName: String? = null,

    @field:Schema(description = "가게 주소", example = "서울시 강남구 테헤란로 123, 2층")
    val storeAddress: String? = null,
)
