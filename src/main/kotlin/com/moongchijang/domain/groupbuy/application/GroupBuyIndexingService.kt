package com.moongchijang.domain.groupbuy.application

import com.moongchijang.domain.groupbuy.application.event.GroupBuyIndexRequestedEvent
import com.moongchijang.domain.groupbuy.application.event.GroupBuyIndexAction
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.search.application.SearchIndexVersionService
import com.moongchijang.domain.search.application.port.VectorIndexDocument
import com.moongchijang.domain.search.application.port.VectorIndexPort
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Service
class GroupBuyIndexingService(
    private val groupBuyRepository: GroupBuyRepository,
    private val embeddingModel: EmbeddingModel,
    private val vectorIndexPort: VectorIndexPort,
    private val searchIndexVersionService: SearchIndexVersionService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val embeddingVersion = "gemini-embedding-001-768-v1"

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: GroupBuyIndexRequestedEvent) {
        process(event)
    }

    fun process(event: GroupBuyIndexRequestedEvent): Boolean {
        if (event.action == GroupBuyIndexAction.DELETE) {
            val deleted = vectorIndexPort.delete(event.groupBuyId)
            if (deleted) {
                searchIndexVersionService.bumpVersion()
                log.info("공구 인덱싱 삭제 완료: id={}", event.groupBuyId)
            } else {
                log.warn("공구 인덱싱 삭제 보류: id={}", event.groupBuyId)
            }
            return deleted
        }

        val groupBuy = groupBuyRepository.findById(event.groupBuyId).orElse(null)
        if (groupBuy == null) {
            log.warn("공구 인덱싱 대상 없음: id={}", event.groupBuyId)
            return true
        }
        return index(groupBuy)
    }

    fun index(groupBuy: GroupBuy): Boolean =
        try {
            val regionLabel = groupBuy.store.region.label
            val storeName = groupBuy.store.name
            val text = "$regionLabel $storeName ${groupBuy.productName}"
            val segment = TextSegment.from(text, Metadata.from("groupBuyId", groupBuy.id.toString()))
            val embedding = embeddingModel.embed(segment).content()
            val indexed = vectorIndexPort.upsert(
                VectorIndexDocument(
                    groupBuyId = groupBuy.id,
                    vectorText = text,
                    vector = embedding.vector(),
                    region = regionLabel,
                    storeName = storeName,
                    productName = groupBuy.productName,
                    status = groupBuy.status.name,
                    deadline = groupBuy.deadline,
                    embeddingVersion = embeddingVersion
                )
            )
            if (indexed) {
                searchIndexVersionService.bumpVersion()
                log.info("공구 임베딩 인덱싱 완료: id={}", groupBuy.id)
            } else {
                log.warn("공구 임베딩 인덱싱 보류: id={}", groupBuy.id)
            }
            indexed
        } catch (e: Exception) {
            log.error("공구 임베딩 인덱싱 실패: id={}, error={}", groupBuy.id, e.message)
            false
        }
}
