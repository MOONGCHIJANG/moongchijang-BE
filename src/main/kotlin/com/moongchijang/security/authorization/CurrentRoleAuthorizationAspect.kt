package com.moongchijang.security.authorization

import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.security.principal.CustomUserPrincipal
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.support.AopUtils
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Aspect
@Component
class CurrentRoleAuthorizationAspect {

    @Before("@annotation(com.moongchijang.security.authorization.RequireCurrentRole) || @within(com.moongchijang.security.authorization.RequireCurrentRole)")
    fun validateCurrentRole(joinPoint: JoinPoint) {
        val requiredRoles = resolveRequiredRoles(joinPoint)
        val principal = SecurityContextHolder.getContext().authentication?.principal as? CustomUserPrincipal
            ?: throw CustomException(ErrorCode.INVALID_LOGIN)

        if (requiredRoles.isEmpty()) {
            return
        }

        if (principal.role !in requiredRoles) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
    }

    private fun resolveRequiredRoles(joinPoint: JoinPoint): Set<UserRole> {
        val method = (joinPoint.signature as MethodSignature).method
        val targetClass = joinPoint.target.javaClass
        val targetMethod = AopUtils.getMostSpecificMethod(method, targetClass)

        val methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(targetMethod, RequireCurrentRole::class.java)
        if (methodAnnotation != null) {
            return methodAnnotation.value.toSet()
        }

        val classAnnotation = AnnotatedElementUtils.findMergedAnnotation(targetClass, RequireCurrentRole::class.java)
        return classAnnotation?.value?.toSet().orEmpty()
    }
}
