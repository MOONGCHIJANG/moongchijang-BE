package com.moongchijang.domain.store.application

import com.moongchijang.domain.store.domain.entity.RecommendedStoreImage
import com.moongchijang.domain.store.domain.repository.RecommendedStoreImageRepository
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class RecommendedStoreImageServiceTest {

    private val recommendedStoreImageRepository: RecommendedStoreImageRepository =
        Mockito.mock(RecommendedStoreImageRepository::class.java)
    private val s3ImageReferenceResolver: S3ImageReferenceResolver =
        Mockito.mock(S3ImageReferenceResolver::class.java)

    private val service = RecommendedStoreImageService(
        recommendedStoreImageRepository = recommendedStoreImageRepository,
        s3ImageReferenceResolver = s3ImageReferenceResolver,
    )

    @Test
    fun `active image keys are resolved to public urls`() {
        Mockito.`when`(recommendedStoreImageRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc())
            .thenReturn(
                listOf(
                    RecommendedStoreImage(
                        imageKey = "dev/group-buys/test/thumbnail/1.jpeg",
                        sortOrder = 1,
                    ),
                    RecommendedStoreImage(
                        imageKey = "dev/group-buys/test/products/2.jpeg",
                        sortOrder = 2,
                    ),
                )
            )
        Mockito.`when`(s3ImageReferenceResolver.resolveForRead("dev/group-buys/test/thumbnail/1.jpeg"))
            .thenReturn("https://cdn.example.com/dev/group-buys/test/thumbnail/1.jpeg")
        Mockito.`when`(s3ImageReferenceResolver.resolveForRead("dev/group-buys/test/products/2.jpeg"))
            .thenReturn("https://cdn.example.com/dev/group-buys/test/products/2.jpeg")

        val result = service.findActiveImageUrls()

        assertThat(result).containsExactly(
            "https://cdn.example.com/dev/group-buys/test/thumbnail/1.jpeg",
            "https://cdn.example.com/dev/group-buys/test/products/2.jpeg",
        )
    }

    @Test
    fun `image url is selected by index with circular fallback`() {
        val imageUrls = listOf("image-1", "image-2")

        assertThat(service.imageUrlByIndex(0, imageUrls)).isEqualTo("image-1")
        assertThat(service.imageUrlByIndex(1, imageUrls)).isEqualTo("image-2")
        assertThat(service.imageUrlByIndex(2, imageUrls)).isEqualTo("image-1")
        assertThat(service.imageUrlByIndex(0, emptyList())).isNull()
    }
}
