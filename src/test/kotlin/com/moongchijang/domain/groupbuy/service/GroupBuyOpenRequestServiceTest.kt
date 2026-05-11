package com.moongchijang.domain.groupbuy.service

import com.moongchijang.domain.groupbuy.application.GroupBuyOpenRequestService
import com.moongchijang.domain.groupbuy.application.dto.CreateGroupBuyOpenRequestRequest
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyOpenRequest
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyOpenRequestRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException

@ExtendWith(MockitoExtension::class)
class GroupBuyOpenRequestServiceTest {

    @Mock
    private lateinit var openRequestRepository: GroupBuyOpenRequestRepository

    @InjectMocks
    private lateinit var service: GroupBuyOpenRequestService

    @Test
    fun `정상 알림 신청 시 저장 성공`() {
        val userId = 1L
        val request = CreateGroupBuyOpenRequestRequest(region = "성수", productName = "소금빵")

        `when`(openRequestRepository.existsByUserIdAndRegionAndProductName(userId, "성수", "소금빵"))
            .thenReturn(false)
        `when`(openRequestRepository.saveAndFlush(any())).thenReturn(
            GroupBuyOpenRequest(userId = userId, region = "성수", productName = "소금빵").apply { id = 1L }
        )

        service.create(userId, request)

        verify(openRequestRepository).saveAndFlush(any())
    }

    @Test
    fun `중복 알림 신청 시 DUPLICATE_OPEN_REQUEST 예외`() {
        val userId = 1L
        val request = CreateGroupBuyOpenRequestRequest(region = "성수", productName = "소금빵")

        `when`(openRequestRepository.existsByUserIdAndRegionAndProductName(userId, "성수", "소금빵"))
            .thenReturn(true)

        val ex = assertThrows<CustomException> { service.create(userId, request) }
        assertEquals(ErrorCode.DUPLICATE_OPEN_REQUEST, ex.errorCode)
        verify(openRequestRepository, never()).saveAndFlush(any())
    }

    @Test
    fun `동시 신청으로 UNIQUE 제약 위반 시 DUPLICATE_OPEN_REQUEST 예외`() {
        val userId = 1L
        val request = CreateGroupBuyOpenRequestRequest(region = "성수", productName = "소금빵")

        `when`(openRequestRepository.existsByUserIdAndRegionAndProductName(userId, "성수", "소금빵"))
            .thenReturn(false)
        `when`(openRequestRepository.saveAndFlush(any<GroupBuyOpenRequest>()))
            .thenThrow(DataIntegrityViolationException("uk_open_req_user_region_product"))

        val ex = assertThrows<CustomException> { service.create(userId, request) }
        assertEquals(ErrorCode.DUPLICATE_OPEN_REQUEST, ex.errorCode)
    }
}
