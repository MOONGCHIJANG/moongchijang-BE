package com.moongchijang.domain.user.infrastructure.business.dto

data class NtsBusinessStatusResponse(
    val status_code: String? = null,
    val data: List<NtsBusinessStatusItem> = emptyList(),
)

data class NtsBusinessStatusItem(
    val b_no: String? = null,
    val b_stt: String? = null,
    val b_stt_cd: String? = null,
    val tax_type: String? = null,
    val tax_type_cd: String? = null,
    val end_dt: String? = null,
)
