package com.moongchijang.domain.store.application

import com.moongchijang.domain.store.domain.repository.RecommendedStoreImageRepository
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RecommendedStoreImageService(
    private val recommendedStoreImageRepository: RecommendedStoreImageRepository,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
) {

    @Transactional(readOnly = true)
    fun findActiveImageUrls(): List<String> {
        return recommendedStoreImageRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc()
            .mapNotNull { s3ImageReferenceResolver.resolveForRead(it.imageKey) }
    }

    fun imageUrlByIndex(index: Int, imageUrls: List<String>): String? {
        if (imageUrls.isEmpty()) {
            return null
        }
        return imageUrls[index.mod(imageUrls.size)]
    }
}
