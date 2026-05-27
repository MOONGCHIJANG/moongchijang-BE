package com.moongchijang.domain.admin.application.dto

data class AdminDashboardSummaryResponse(
    val pendingRefundAmount: Long,
    val pendingRefundAmountChangeRate: Double,
    val pendingApprovalCount: Long,
    val averageReviewMinutes: Long,
    val pendingApprovalChangeRate: Double,
    val unconfirmedOrderCount: Long,
    val unconfirmedOrderOver48hCount: Long,
    val todayCompletedRefundCount: Long,
    val todayCompletedApprovalCount: Long,
    val hasOrderOver48h: Boolean,
)
