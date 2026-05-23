package com.moongchijang.domain.pickup.presentation

import com.moongchijang.domain.participation.domain.entity.PickupStatus
import com.moongchijang.domain.pickup.application.PickupService
import com.moongchijang.domain.pickup.application.dto.NearestPickupQrResponse
import com.moongchijang.domain.pickup.application.dto.PickupVerifyResponse
import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.security.principal.CustomUserPrincipal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDateTime

class PickupControllerTest {

    private val pickupService: PickupService = mock(PickupService::class.java)
    private val controller = PickupController(pickupService)

    @Test
    fun `인증 사용자는 가장 가까운 QR 후보를 조회할 수 있다`() {
        val response = NearestPickupQrResponse(
            hasCandidate = false,
            hasMultipleToday = false,
            reason = null,
            item = null,
        )
        `when`(pickupService.getNearestPickupQr(1L)).thenReturn(response)

        val result = controller.getNearestPickupQr(principal(UserRole.BUYER, id = 1L))

        assertEquals(response, result.body?.data)
        verify(pickupService).getNearestPickupQr(1L)
    }

    @Test
    fun `SELLER는 QR 검증을 호출할 수 있다`() {
        val response = PickupVerifyResponse(
            participationId = 99L,
            pickupStatus = PickupStatus.PICKED_UP,
            pickedUpAt = LocalDateTime.now(),
            pickupProcessedByUserId = 7L,
        )
        `when`(pickupService.verifyPickup("qr-token", 7L)).thenReturn(response)

        val result = controller.verifyPickup("qr-token", principal(UserRole.SELLER, id = 7L))

        assertEquals(response, result.body?.data)
        verify(pickupService).verifyPickup("qr-token", 7L)
    }

    @Test
    fun `ADMIN은 QR 검증을 호출할 수 있다`() {
        val response = PickupVerifyResponse(
            participationId = 99L,
            pickupStatus = PickupStatus.PICKED_UP,
            pickedUpAt = LocalDateTime.now(),
            pickupProcessedByUserId = 8L,
        )
        `when`(pickupService.verifyPickup("qr-token", 8L)).thenReturn(response)

        val result = controller.verifyPickup("qr-token", principal(UserRole.ADMIN, id = 8L))

        assertEquals(response, result.body?.data)
        verify(pickupService).verifyPickup("qr-token", 8L)
    }

    private fun principal(role: UserRole, id: Long): CustomUserPrincipal =
        CustomUserPrincipal(
            id = id,
            email = "$id@example.com",
            role = role,
        )
}
