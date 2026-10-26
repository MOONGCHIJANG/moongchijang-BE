package com.moongchijang.domain.product.application

import com.moongchijang.domain.product.application.dto.ProductShareResponse
import com.moongchijang.domain.product.domain.repository.ProductRepository
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductShareService(
    private val productRepository: ProductRepository,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
    @Value("\${app.share.base-url}") private val shareBaseUrl: String,
) {

    @Transactional(readOnly = true)
    fun getShareMeta(productId: Long): ProductShareResponse {
        val product = productRepository.findWithStoreById(productId)
            ?: throw CustomException(ErrorCode.PRODUCT_NOT_FOUND)

        return ProductShareResponse(
            productId = product.id,
            productName = product.name,
            storeName = product.store.name,
            imageUrl = s3ImageReferenceResolver.resolveForRead(product.thumbnailKey),
            shareUrl = "${shareBaseUrl.trimEnd('/')}/products/${product.id}",
        )
    }
}
