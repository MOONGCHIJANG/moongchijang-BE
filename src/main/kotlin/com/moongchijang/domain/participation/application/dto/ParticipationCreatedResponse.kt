package com.moongchijang.domain.participation.application.dto

data class ParticipationCreatedResponse(
    val participationId: Long,
    val orderName: String,
    val totalAmount: Int,
    val productAmount: Int,
    val feeAmount: Int
)
