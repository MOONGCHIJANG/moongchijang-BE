package com.moongchijang.domain.search.infrastructure.demo

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import kotlin.math.sqrt

class LocalDemoEmbeddingModel(
    private val vectorSize: Int = 768
) : EmbeddingModel {
    override fun embed(text: String): Response<Embedding> =
        Response.from(Embedding.from(vectorize(text)))

    override fun embed(textSegment: TextSegment): Response<Embedding> =
        Response.from(Embedding.from(vectorize(textSegment.text())))

    override fun embedAll(textSegments: List<TextSegment>): Response<List<Embedding>> =
        Response.from(textSegments.map { TextSegment ->
            Embedding.from(vectorize(TextSegment.text()))
        })

    override fun dimension(): Int = vectorSize

    override fun modelName(): String = "local-demo-deterministic-embedding"

    private fun vectorize(text: String): FloatArray {
        val canonical = LocalDemoSearchVocabulary.embeddingText(text)
        val compact = canonical.replace(" ", "")
        val features = buildList {
            if (canonical.isNotBlank()) add(canonical)
            if (compact.isNotBlank()) add(compact)
            canonical.split(" ")
                .filter { it.isNotBlank() }
                .forEach { token ->
                    add(token)
                    if (token.length >= 2) {
                        token.windowed(2, 1, partialWindows = false).forEach { add(it) }
                    }
                    if (token.length >= 3) {
                        token.windowed(3, 1, partialWindows = false).forEach { add(it) }
                    }
                }
        }

        val vector = FloatArray(vectorSize)
        features.forEach { feature ->
            val hash = feature.hashCode()
            addFeature(vector, hash, 3.0f)
            addFeature(vector, hash * 31 + 7, 1.5f)
            addFeature(vector, hash * 131 + 17, 0.75f)
        }

        normalize(vector)
        return vector
    }

    private fun addFeature(vector: FloatArray, hash: Int, weight: Float) {
        val index = Math.floorMod(hash, vector.size)
        vector[index] += weight
    }

    private fun normalize(vector: FloatArray) {
        val norm = sqrt(vector.fold(0.0) { acc, value -> acc + value * value })
        if (norm == 0.0) return
        for (i in vector.indices) {
            vector[i] = (vector[i] / norm).toFloat()
        }
    }
}
