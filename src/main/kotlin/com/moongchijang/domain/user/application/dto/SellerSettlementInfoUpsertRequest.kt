package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "사장님 정산 정보 입력 요청")
data class SellerSettlementInfoUpsertRequest(

    @field:NotBlank(message = "은행은 필수입니다.")
    @field:Size(max = 80, message = "은행값은 80자 이하여야 합니다.")
    @field:Schema(description = "은행/증권사 선택값", example = "KB국민")
    val bankCode: String,

    @field:NotBlank(message = "계좌번호는 필수입니다.")
    @field:Size(max = 50, message = "계좌번호는 50자 이하여야 합니다.")
    @field:Schema(description = "계좌번호", example = "000-000-0000")
    val accountNumber: String,

    @field:NotBlank(message = "예금주명은 필수입니다.")
    @field:Size(max = 50, message = "예금주명은 50자 이하여야 합니다.")
    @field:Schema(description = "예금주명", example = "홍길동")
    val accountHolderName: String,
)
