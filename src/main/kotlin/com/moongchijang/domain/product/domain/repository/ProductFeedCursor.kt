package com.moongchijang.domain.product.domain.repository

import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import java.time.LocalDateTime

sealed interface ProductFeedCursor {
    val id: Long
    val sort: ProductFeedSort

    fun encode(): String = when (this) {
        is Latest -> "${sort.name}|$createdAt|$id"
        is PriceLow -> "${sort.name}|$unitPrice|$id"
        is MinQuantity -> "${sort.name}|$minQuantity|$id"
    }

    data class Latest(
        val createdAt: LocalDateTime,
        override val id: Long,
    ) : ProductFeedCursor {
        override val sort: ProductFeedSort = ProductFeedSort.LATEST
    }

    data class PriceLow(
        val unitPrice: Int,
        override val id: Long,
    ) : ProductFeedCursor {
        override val sort: ProductFeedSort = ProductFeedSort.PRICE_LOW
    }

    data class MinQuantity(
        val minQuantity: Int,
        override val id: Long,
    ) : ProductFeedCursor {
        override val sort: ProductFeedSort = ProductFeedSort.MIN_QUANTITY
    }

    companion object {
        fun decode(value: String, expectedSort: ProductFeedSort): ProductFeedCursor {
            val parts = value.split("|", limit = 3)
            if (parts.size != 3) {
                invalidCursor()
            }

            val cursorSort = runCatching { ProductFeedSort.valueOf(parts[0]) }
                .getOrElse { invalidCursor() }
            if (cursorSort != expectedSort) {
                invalidCursor()
            }

            val id = parts[2].toLongOrNull()?.takeIf { it > 0L } ?: invalidCursor()
            return when (cursorSort) {
                ProductFeedSort.LATEST -> Latest(
                    createdAt = runCatching { LocalDateTime.parse(parts[1]) }.getOrElse { invalidCursor() },
                    id = id,
                )
                ProductFeedSort.PRICE_LOW -> PriceLow(
                    unitPrice = parts[1].toIntOrNull()?.takeIf { it >= 0 } ?: invalidCursor(),
                    id = id,
                )
                ProductFeedSort.MIN_QUANTITY -> MinQuantity(
                    minQuantity = parts[1].toIntOrNull()?.takeIf { it >= 0 } ?: invalidCursor(),
                    id = id,
                )
            }
        }

        private fun invalidCursor(): Nothing {
            throw CustomException(ErrorCode.INVALID_INPUT, "product feed cursor is invalid")
        }
    }
}
