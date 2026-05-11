package com.moongchijang.domain.groupbuy.infrastructure.event

import com.moongchijang.domain.groupbuy.application.event.GroupBuyIndexAction
import com.moongchijang.domain.groupbuy.application.event.GroupBuyIndexRequestedEvent
import com.moongchijang.domain.groupbuy.application.port.GroupBuyIndexingEventPublisher
import com.moongchijang.global.config.IndexingProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

@Component
@ConditionalOnProperty(prefix = "indexing", name = ["publisher"], havingValue = "sqs")
class SqsGroupBuyIndexingEventPublisher(
    private val objectMapper: ObjectMapper,
    private val properties: IndexingProperties
) : GroupBuyIndexingEventPublisher {
    private val log = LoggerFactory.getLogger(javaClass)
    private val sqsClient: SqsClient = SqsClient.create()

    override fun publishIndexRequested(groupBuyId: Long, action: GroupBuyIndexAction) {
        val queueUrl = properties.sqs.queueUrl
        if (queueUrl.isBlank()) {
            log.warn("SQS 인덱싱 이벤트 발행 생략: queueUrl 미설정, groupBuyId={}", groupBuyId)
            return
        }

        val event = GroupBuyIndexRequestedEvent(groupBuyId, action)
        val requestBuilder = SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(objectMapper.writeValueAsString(event))

        properties.sqs.messageGroupId.takeIf { it.isNotBlank() }?.let {
            requestBuilder.messageGroupId(it)
        }

        sqsClient.sendMessage(requestBuilder.build())
        log.info("SQS 인덱싱 이벤트 발행 완료: groupBuyId={}, action={}", groupBuyId, action)
    }
}
