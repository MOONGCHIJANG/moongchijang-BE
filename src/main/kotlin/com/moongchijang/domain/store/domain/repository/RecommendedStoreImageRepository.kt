package com.moongchijang.domain.store.domain.repository

import com.moongchijang.domain.store.domain.entity.RecommendedStoreImage
import org.springframework.data.jpa.repository.JpaRepository

interface RecommendedStoreImageRepository : JpaRepository<RecommendedStoreImage, Long> {

    fun findAllByActiveTrueOrderBySortOrderAscIdAsc(): List<RecommendedStoreImage>
}
