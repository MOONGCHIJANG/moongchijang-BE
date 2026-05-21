package com.moongchijang.domain.groupbuy.application.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class GroupBuyRequestCreateRequest(

    @NotBlank(message = "매장명은 필수입니다")
    @Size(max = 100, message = "매장명은 100자 이하이어야 합니다")
    val storeName: String,

    @Size(max = 200, message = "매장 주소는 200자 이하이어야 합니다")
    val storeAddress: String? = null,

    @Size(max = 100, message = "외부 장소 ID는 100자 이하이어야 합니다")
    val placeId: String? = null,

    @Size(max = 200, message = "도로명 주소는 200자 이하이어야 합니다")
    val roadAddress: String? = null,

    @Size(max = 200, message = "지번 주소는 200자 이하이어야 합니다")
    val lotAddress: String? = null,

    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90 이하이어야 합니다")
    val latitude: Double? = null,

    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180 이하이어야 합니다")
    val longitude: Double? = null,

    @NotBlank(message = "베이커리/상품명은 필수입니다")
    @Size(max = 100, message = "상품명은 100자 이하이어야 합니다")
    val productName: String,

    @NotNull(message = "희망 참여 수량은 필수입니다")
    @Min(value = 1, message = "희망 참여 수량은 1개 이상이어야 합니다")
    val desiredQuantity: Int,

    @NotNull(message = "희망 픽업 날짜는 필수입니다")
    val desiredPickupDate: LocalDate,

    @Size(max = 500, message = "추가 요청사항은 500자 이하이어야 합니다")
    val additionalNote: String? = null,

    @Size(max = 20, message = "연락처는 20자 이하이어야 합니다")
    val contactPhone: String? = null,

    @Size(max = 50, message = "인스타그램 계정은 50자 이하이어야 합니다")
    val contactInstagram: String? = null
)
