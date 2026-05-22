package com.moongchijang.domain.notification.domain.entity

enum class NotificationTriggerType {
    PICKUP_SAME_DAY_MORNING,              // ①
    PICKUP_DAY_BEFORE_MORNING,            // ②
    PICKUP_COMPLETED_IMMEDIATE,           // ③-1
    PICKUP_NOT_COMPLETED_AFTER_CUTOFF,    // ③-2
    WISH_DEADLINE_MINUS_3_DAYS,           // ④
    WISH_DEADLINE_MINUS_1_DAY,            // ⑤
    WISH_TARGET_ACHIEVED_IMMEDIATE,       // ⑥
    APPLY_PAYMENT_SUCCESS_IMMEDIATE,      // ⑦
    APPLY_GROUPBUY_ACHIEVED_IMMEDIATE,    // ⑧
    APPLY_GROUPBUY_FAILED_IMMEDIATE,      // ⑨
    REQUEST_OPENED_IMMEDIATE,             // ⑩
    REQUEST_REJECTED_IMMEDIATE,           // ⑪
    REQUEST_NEW_PARTICIPANT_IMMEDIATE,    // ⑫
    REQUEST_TARGET_ACHIEVED_IMMEDIATE,    // ⑬
    REQUEST_DEADLINE_MINUS_3_DAYS         // ⑭
}
