package com.moongchijang.domain.groupbuy.application.dto

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyRequestStatus
import com.moongchijang.domain.store.domain.entity.DistrictType
import com.moongchijang.domain.store.domain.entity.RegionType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AdminGroupBuyRequestApproveRequest(

    val storeId: Long? = null,

    @field:Size(max = 100, message = "매장명은 100자 이하이어야 합니다")
    val storeName: String? = null,

    @field:Size(max = 200, message = "매장 주소는 200자 이하이어야 합니다")
    val storeAddress: String? = null,

    @field:Size(max = 20, message = "매장 연락처는 20자 이하이어야 합니다")
    val storePhoneNumber: String? = null,

    val region: RegionType? = null,

    val district: DistrictType? = null,

    val latitude: Double? = null,

    val longitude: Double? = null,

    @field:NotBlank(message = "공구 제목은 필수입니다")
    @field:Size(max = 100, message = "공구 제목은 100자 이하이어야 합니다")
    val productName: String,

    @field:NotBlank(message = "공구 내용은 필수입니다")
    @field:Size(max = 1000, message = "공구 내용은 1000자 이하이어야 합니다")
    val productDescription: String,

    @field:Min(value = 1, message = "정가는 1원 이상이어야 합니다")
    val originalPrice: Int? = null,

    @field:NotNull(message = "공구가는 필수입니다")
    @field:Min(value = 1, message = "공구가는 1원 이상이어야 합니다")
    val price: Int,

    @field:NotNull(message = "목표 수량은 필수입니다")
    @field:Min(value = 1, message = "목표 수량은 1개 이상이어야 합니다")
    val targetQuantity: Int,

    @field:Min(value = 1, message = "최대 수량은 1개 이상이어야 합니다")
    val maxQuantity: Int? = null,

    @field:Min(value = 1, message = "1인 구매 제한은 1개 이상이어야 합니다")
    val perUserLimit: Int? = null,

    @field:NotEmpty(message = "상품 이미지는 최소 1장 이상 필요합니다")
    @field:Size(max = 5, message = "상품 이미지는 최대 5장까지 등록할 수 있습니다")
    val imageUrls: List<@NotBlank(message = "이미지 URL은 비어 있을 수 없습니다") String>,

    @field:NotNull(message = "모집 시작일시는 필수입니다")
    val recruitmentStartAt: LocalDateTime,

    @field:NotNull(message = "모집 마감일시는 필수입니다")
    val deadline: LocalDateTime,

    @field:NotNull(message = "픽업일은 필수입니다")
    val pickupDate: LocalDate,

    @field:NotNull(message = "픽업 시작 시간은 필수입니다")
    val pickupTimeStart: LocalTime,

    @field:NotNull(message = "픽업 종료 시간은 필수입니다")
    val pickupTimeEnd: LocalTime,

    @field:NotBlank(message = "픽업 장소는 필수입니다")
    @field:Size(max = 200, message = "픽업 장소는 200자 이하이어야 합니다")
    val pickupLocation: String,

    @field:Size(max = 20, message = "픽업 연락처는 20자 이하이어야 합니다")
    val pickupContact: String? = null
)

data class AdminGroupBuyRequestRejectRequest(

    @field:NotBlank(message = "반려 사유는 필수입니다")
    @field:Size(max = 200, message = "반려 사유는 200자 이하이어야 합니다")
    val rejectionReason: String
)

data class AdminGroupBuyRequestActionResponse(
    val requestId: Long,
    val status: GroupBuyRequestStatus,
    val groupBuyId: Long? = null
)
