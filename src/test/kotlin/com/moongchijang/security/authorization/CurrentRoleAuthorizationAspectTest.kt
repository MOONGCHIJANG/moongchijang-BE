package com.moongchijang.security.authorization

import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.security.principal.CustomUserPrincipal
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class CurrentRoleAuthorizationAspectTest {

    private val aspect = CurrentRoleAuthorizationAspect()

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `메서드에 허용된 current role일 때 통과`() {
        authenticateAs(UserRole.SELLER)

        assertDoesNotThrow {
            aspect.validateCurrentRole(joinPointFor(SellerMethodProtectedController::class.java, "sellerOnly"))
        }
    }

    @Test
    fun `클래스에 선언된 current role과 다르면 FORBIDDEN 예외`() {
        authenticateAs(UserRole.BUYER)

        val exception = assertThrows<CustomException> {
            aspect.validateCurrentRole(joinPointFor(AdminClassProtectedController::class.java, "dashboard"))
        }

        assertEquals(ErrorCode.FORBIDDEN, exception.errorCode)
    }

    @Test
    fun `인증 정보가 없으면 INVALID_LOGIN 예외`() {
        val exception = assertThrows<CustomException> {
            aspect.validateCurrentRole(joinPointFor(SellerMethodProtectedController::class.java, "sellerOnly"))
        }

        assertEquals(ErrorCode.INVALID_LOGIN, exception.errorCode)
    }

    private fun authenticateAs(role: UserRole) {
        val principal = CustomUserPrincipal(
            id = 1L,
            email = "test@example.com",
            role = role,
        )
        val authentication = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun joinPointFor(targetClass: Class<*>, methodName: String): JoinPoint {
        val method = targetClass.declaredMethods.first { it.name == methodName }
        val methodSignature = mock(MethodSignature::class.java)
        `when`(methodSignature.method).thenReturn(method)

        val joinPoint = mock(JoinPoint::class.java)
        `when`(joinPoint.signature).thenReturn(methodSignature)
        `when`(joinPoint.target).thenReturn(targetClass.getDeclaredConstructor().newInstance())
        return joinPoint
    }

    class SellerMethodProtectedController {
        @RequireCurrentRole(UserRole.SELLER)
        fun sellerOnly() = Unit
    }

    @RequireCurrentRole(UserRole.ADMIN)
    class AdminClassProtectedController {
        fun dashboard() = Unit
    }
}
