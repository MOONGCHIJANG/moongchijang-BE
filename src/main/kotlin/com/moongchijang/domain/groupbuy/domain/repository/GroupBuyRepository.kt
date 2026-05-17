package com.moongchijang.domain.groupbuy.domain.repository

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional

interface GroupBuyRepository : JpaRepository<GroupBuy, Long>, GroupBuyRepositoryCustom {

    @EntityGraph(attributePaths = ["store"])
    fun findWithStoreById(id: Long): Optional<GroupBuy>

    /**
     * ngram FULLTEXT 인덱스를 사용한 공구 검색.
     *
     * group_buys.product_name 매칭과 stores.name/address 매칭을 UNION 으로 합쳐
     * 중복 제거한 뒤, status/deadline 으로 노출 가능한 공구만 반환한다.
     *
     * @param query   FullTextQueryBuilder.toBooleanQuery 로 변환된 BOOLEAN MODE 쿼리. 빈 문자열이면 빈 결과.
     * @param status  노출 허용 상태 (보통 IN_PROGRESS)
     * @param now     마감 비교 기준 시각 (deadline > now 인 공구만 반환)
     * @param limit   반환할 최대 결과 수
     */
    @Query(
        value = """
            SELECT * FROM (
                SELECT gb.* FROM group_buys gb
                WHERE MATCH(gb.product_name) AGAINST(:query IN BOOLEAN MODE)
                UNION
                SELECT gb.* FROM group_buys gb
                JOIN stores s ON gb.store_id = s.id
                WHERE MATCH(s.name, s.address) AGAINST(:query IN BOOLEAN MODE)
            ) AS merged
            WHERE merged.status = :status
              AND merged.deadline > :now
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun searchByFullText(
        @Param("query") query: String,
        @Param("status") status: String,
        @Param("now") now: LocalDateTime,
        @Param("limit") limit: Int,
    ): List<GroupBuy>
}
