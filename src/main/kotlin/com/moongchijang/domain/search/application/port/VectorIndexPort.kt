package com.moongchijang.domain.search.application.port

import java.time.LocalDateTime

data class VectorIndexDocument(
    val groupBuyId: Long,
    val vectorText: String,
    val vector: FloatArray,
    val region: String,
    val storeName: String,
    val productName: String,
    val status: String,
    val deadline: LocalDateTime,
    val embeddingVersion: String
)

interface VectorIndexPort {
    fun upsert(document: VectorIndexDocument): Boolean

    fun delete(groupBuyId: Long): Boolean
}
