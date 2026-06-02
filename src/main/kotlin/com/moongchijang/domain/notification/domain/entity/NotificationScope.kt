package com.moongchijang.domain.notification.domain.entity

import com.moongchijang.domain.user.domain.entity.UserRole

enum class NotificationScope {
    BUYER,
    OWNER;

    companion object {
        fun from(role: UserRole): NotificationScope {
            return when (role) {
                UserRole.BUYER -> BUYER
                UserRole.SELLER -> OWNER
                UserRole.ADMIN -> BUYER
            }
        }
    }
}
