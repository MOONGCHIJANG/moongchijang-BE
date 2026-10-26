package com.moongchijang.domain.product.domain.repository

import com.moongchijang.domain.product.domain.entity.Product
import com.moongchijang.domain.product.domain.entity.ProductStatus
import com.moongchijang.domain.store.domain.entity.StoreSalesStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductRepository : JpaRepository<Product, Long>, ProductRepositoryCustom {

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

    @Query(
        value = """
            SELECT p.id
            FROM products p
            JOIN stores s ON p.store_id = s.id
            WHERE MATCH(p.name) AGAINST(:query IN BOOLEAN MODE)
              AND p.status = :productStatus
              AND s.sales_status = :storeSalesStatus
            ORDER BY p.created_at DESC, p.id DESC
        """,
        nativeQuery = true,
    )
    fun searchProductIdsByFullText(
        @Param("query") query: String,
        @Param("productStatus") productStatus: String,
        @Param("storeSalesStatus") storeSalesStatus: String,
    ): List<Long>

    @Query(
        value = """
            SELECT p.id
            FROM products p
            JOIN stores s ON p.store_id = s.id
            WHERE MATCH(s.name, s.address) AGAINST(:query IN BOOLEAN MODE)
              AND p.status = :productStatus
              AND s.sales_status = :storeSalesStatus
            ORDER BY p.created_at DESC, p.id DESC
        """,
        nativeQuery = true,
    )
    fun searchStoreProductIdsByFullText(
        @Param("query") query: String,
        @Param("productStatus") productStatus: String,
        @Param("storeSalesStatus") storeSalesStatus: String,
    ): List<Long>

    @Query(
        value = """
            SELECT keyword
            FROM (
                SELECT p.name AS keyword, p.created_at AS created_at
                FROM products p
                JOIN stores s ON p.store_id = s.id
                WHERE p.status = :productStatus
                  AND s.sales_status = :storeSalesStatus
                  AND p.name <> ''
                UNION ALL
                SELECT s.name AS keyword, p.created_at AS created_at
                FROM products p
                JOIN stores s ON p.store_id = s.id
                WHERE p.status = :productStatus
                  AND s.sales_status = :storeSalesStatus
                  AND s.name <> ''
                UNION ALL
                SELECT s.address AS keyword, p.created_at AS created_at
                FROM products p
                JOIN stores s ON p.store_id = s.id
                WHERE p.status = :productStatus
                  AND s.sales_status = :storeSalesStatus
                  AND s.address <> ''
            ) active_keywords
            GROUP BY keyword
            ORDER BY MAX(created_at) DESC
            LIMIT :limit
        """,
        nativeQuery = true,
    )
    fun findActiveSearchKeywords(
        @Param("productStatus") productStatus: String,
        @Param("storeSalesStatus") storeSalesStatus: String,
        @Param("limit") limit: Int,
    ): List<String>
}
