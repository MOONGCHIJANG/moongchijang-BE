package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "사장님 사업자 정보 입력 요청")
data class SellerBusinessInfoUpsertRequest(

    @field:NotBlank(message = "사업자등록번호는 필수입니다.")
    @field:Pattern(
        regexp = "^\\d{3}-?\\d{2}-?\\d{5}$",
        message = "올바른 사업자등록번호를 입력해주세요.",
    )
    @field:Schema(description = "사업자등록번호", example = "111-22-33333")
    val businessRegistrationNumber: String,

    @field:NotBlank(message = "가게명은 필수입니다.")
    @field:Size(max = 100, message = "가게명은 100자 이하여야 합니다.")
    @field:Schema(description = "가게명", example = "뭉치장베이커리")
    val storeName: String,

    @field:NotBlank(message = "대표자명은 필수입니다.")
    @field:Size(max = 50, message = "대표자명은 50자 이하여야 합니다.")
    @field:Schema(description = "대표자명", example = "홍길동")
    val ownerName: String,

    @field:NotBlank(message = "가게주소는 필수입니다.")
    @field:Size(max = 255, message = "가게주소는 255자 이하여야 합니다.")
    @field:Schema(description = "가게주소", example = "서울시 강남구 테헤란로 123, 2층")
    val storeAddress: String,

    @field:NotBlank(message = "전화번호는 필수입니다.")
    @field:Pattern(
        regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$",
        message = "올바른 전화번호를 입력해주세요.",
    )
    @field:Schema(description = "전화번호", example = "010-1234-5678")
    val phoneNumber: String,
)
