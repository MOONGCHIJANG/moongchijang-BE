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
    REQUEST_DEADLINE_MINUS_3_DAYS,            // ⑭
    OWNER_PICKUP_SAME_DAY_REMINDER,           // ⑮
    OWNER_PICKUP_DAY_BEFORE_REMINDER,         // ⑯
    OWNER_GROUPBUY_ACHIEVED,                  // ⑰
    OWNER_GROUPBUY_FAILED,                    // ⑱
    OWNER_CLOSE_REQUEST_APPROVED,             // ⑲
    OWNER_CLOSE_REQUEST_REJECTED,             // ⑳
    OWNER_OPEN_REQUEST_APPROVED,              // ㉑
    OWNER_OPEN_REQUEST_REJECTED,              // ㉒
    OWNER_ORDER_CONFIRM_REQUIRED,             // ㉓
    OWNER_ORDER_CANCELLED                     // ㉔
}
