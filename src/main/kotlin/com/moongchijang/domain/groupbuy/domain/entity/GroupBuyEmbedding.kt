package com.moongchijang.domain.groupbuy.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "group_buy_embeddings",
    indexes = [Index(name = "idx_group_buy_embeddings_group_buy_id", columnList = "group_buy_id")]
)
class GroupBuyEmbedding(
    @Column(name = "group_buy_id", nullable = false, unique = true)
    val groupBuyId: Long,

    @Column(columnDefinition = "JSON", nullable = false)
    var embedding: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L
) : BaseEntity()
