package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 사업자 정보 조회 응답")
data class SellerBusinessProfileResponse(

    @field:Schema(description = "사업자등록번호", example = "1112233333")
    val businessRegistrationNumber: String,

    @field:Schema(description = "가게명", example = "뭉치장베이커리")
    val storeName: String,

    @field:Schema(description = "대표자명", example = "홍길동")
    val ownerName: String,

    @field:Schema(description = "가게주소", example = "서울시 강남구 테헤란로 123, 2층")
    val storeAddress: String,

    @field:Schema(description = "연락처", example = "010-1234-5678")
    val phoneNumber: String,
)
