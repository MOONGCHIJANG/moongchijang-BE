package com.moongchijang.domain.user.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "사장님 입금 계좌 정보 조회 응답")
data class SellerSettlementAccountResponse(

    @field:Schema(description = "은행/증권사 코드", example = "KOOKMIN")
    val bankCode: String,

    @field:Schema(description = "계좌번호", example = "000-000-0000")
    val accountNumber: String,

    @field:Schema(description = "예금주명", example = "홍길동")
    val accountHolderName: String,
)
