package com.moongchijang.domain.product.domain.repository

import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStatus
import com.moongchijang.domain.store.domain.entity.StoreSalesStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductRepository : JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p JOIN FETCH p.store WHERE p.id = :productId")
    fun findWithStoreById(@Param("productId") productId: Long): Product?

    @Query(
        """
        SELECT p
        FROM Product p
        JOIN FETCH p.store s
        WHERE p.status = :productStatus
          AND s.salesStatus = :storeSalesStatus
        ORDER BY p.createdAt DESC, p.id DESC
        """
    )
    fun findAllSaleable(
        @Param("productStatus") productStatus: ProductStatus = ProductStatus.ACTIVE,
        @Param("storeSalesStatus") storeSalesStatus: StoreSalesStatus = StoreSalesStatus.ACTIVE,
    ): List<Product>
}
