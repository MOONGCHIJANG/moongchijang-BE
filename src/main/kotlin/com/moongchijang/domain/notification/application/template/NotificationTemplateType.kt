package com.moongchijang.domain.notification.application.template

enum class NotificationTemplateType {
    PICKUP_SAME_DAY_REMINDER,                 // ①
    PICKUP_DAY_BEFORE_REMINDER,               // ②
    PICKUP_INCOMPLETE_AFTER_CUTOFF,           // ③
    WISH_DEADLINE_MINUS_3_DAYS,               // ④
    WISH_DEADLINE_MINUS_1_DAY,                // ⑤
    WISH_TARGET_ACHIEVED,                     // ⑥
    APPLY_PAYMENT_SUCCESS,                    // ⑦
    APPLY_GROUPBUY_ACHIEVED,                  // ⑧
    APPLY_GROUPBUY_FAILED,                    // ⑨
    REQUEST_OPENED,                           // ⑩
    REQUEST_REJECTED,                         // ⑪
    REQUEST_NEW_PARTICIPANT,                  // ⑫
    REQUEST_TARGET_ACHIEVED,                  // ⑬
    REQUEST_DEADLINE_MINUS_3_DAYS             // ⑭
}
