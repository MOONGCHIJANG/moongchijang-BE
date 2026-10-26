package com.moongchijang.domain.product.repository

import com.moongchijang.domain.product.domain.repository.ProductFeedCursor
import com.moongchijang.domain.product.domain.repository.ProductFeedSort
import com.moongchijang.global.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class ProductFeedCursorTest {

    @Test
    fun `최신순 커서를 인코딩하고 디코딩한다`() {
        val cursor = ProductFeedCursor.Latest(
            createdAt = LocalDateTime.of(2026, 10, 26, 10, 30),
            id = 15L,
        )

        val decoded = ProductFeedCursor.decode(cursor.encode(), ProductFeedSort.LATEST)

        assertThat(decoded).isEqualTo(cursor)
    }

    @Test
    fun `요청 정렬과 다른 커서는 거부한다`() {
        val cursor = ProductFeedCursor.PriceLow(unitPrice = 3_000, id = 15L)

        assertThrows<CustomException> {
            ProductFeedCursor.decode(cursor.encode(), ProductFeedSort.LATEST)
        }
    }
}
