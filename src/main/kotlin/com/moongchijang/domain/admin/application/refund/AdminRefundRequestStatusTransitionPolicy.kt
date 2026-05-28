package com.moongchijang.domain.admin.application.refund

import com.moongchijang.domain.admin.application.dto.refund.AdminRefundRequestStatus
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode

object AdminRefundRequestStatusTransitionPolicy {
    private val allowedTransitions = mapOf(
        AdminRefundRequestStatus.REVIEW_PENDING to setOf(
            AdminRefundRequestStatus.IN_PROGRESS,
            AdminRefundRequestStatus.APPROVED,
            AdminRefundRequestStatus.REJECTED,
        ),
        AdminRefundRequestStatus.IN_PROGRESS to setOf(
            AdminRefundRequestStatus.APPROVED,
            AdminRefundRequestStatus.REJECTED,
        ),
        AdminRefundRequestStatus.APPROVED to emptySet(),
        AdminRefundRequestStatus.REJECTED to emptySet(),
    )

    fun validateTransition(from: AdminRefundRequestStatus, to: AdminRefundRequestStatus) {
        if (from == to) {
            return
        }

        if (from == AdminRefundRequestStatus.APPROVED || from == AdminRefundRequestStatus.REJECTED) {
            throw CustomException(ErrorCode.ADMIN_REFUND_REQUEST_ALREADY_PROCESSED)
        }

        val allowed = allowedTransitions[from].orEmpty()
        if (to !in allowed) {
            throw CustomException(ErrorCode.ADMIN_REFUND_REQUEST_INVALID_STATUS_TRANSITION)
        }
    }
}
