package com.moongchijang.domain.search.domain

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "search_corrections",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_search_corrections_source_keyword",
            columnNames = ["source_keyword"]
        )
    ]
)
class SearchCorrection(
    @Column(name = "source_keyword", nullable = false, length = 100)
    val sourceKeyword: String,

    @Column(name = "target_keyword", nullable = false, length = 100)
    val targetKeyword: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: SearchCorrectionType,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "hit_count", nullable = false)
    var hitCount: Long = 0L,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity() {
    fun recordHit() {
        hitCount += 1
    }
}
