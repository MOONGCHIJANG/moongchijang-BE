package com.moongchijang.security.authorization

import com.moongchijang.domain.user.domain.entity.UserRole

@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequireCurrentRole(
    vararg val value: UserRole,
)
