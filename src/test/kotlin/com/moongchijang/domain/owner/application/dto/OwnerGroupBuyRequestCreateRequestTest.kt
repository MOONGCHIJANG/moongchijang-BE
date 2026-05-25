package com.moongchijang.domain.owner.application.dto

import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class OwnerGroupBuyRequestCreateRequestTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `상품 설명은 30자까지 허용한다`() {
        val request = validRequest(productDescription = "가".repeat(30))

        val violations = validator.validate(request)

        assertTrue(violations.none { it.propertyPath.toString() == "productDescription" })
    }

    @Test
    fun `상품 설명이 30자를 초과하면 검증에 실패한다`() {
        val request = validRequest(productDescription = "가".repeat(31))

        val violations = validator.validate(request)

        val productDescriptionViolation = violations.single { it.propertyPath.toString() == "productDescription" }
        assertEquals("상품 설명은 30자 이하이어야 합니다", productDescriptionViolation.message)
    }

    private fun validRequest(
        productDescription: String
    ) = OwnerGroupBuyRequestCreateRequest(
        storeId = 1L,
        productName = "두쫀쿠 세트",
        productDescription = productDescription,
        deadline = LocalDateTime.of(2026, 6, 3, 23, 59),
        originalPrice = 12000,
        price = 9900,
        targetQuantity = 20,
        maxQuantity = 50,
        perUserLimit = 2,
        imageUrls = listOf("https://cdn.example.com/1.jpg"),
        pickupDate = LocalDate.of(2026, 6, 4),
        pickupTimeStart = LocalTime.of(12, 0),
        pickupTimeEnd = LocalTime.of(18, 0),
        pickupLocation = "서울 성동구 성수이로 1",
        pickupContact = "01012345678"
    )
}
