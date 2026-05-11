package com.moongchijang.domain.groupbuy.infrastructure.event

import com.moongchijang.domain.groupbuy.application.GroupBuyIndexingService
import com.moongchijang.domain.groupbuy.application.event.GroupBuyIndexRequestedEvent
import com.moongchijang.global.config.IndexingProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest

@Component
@ConditionalOnProperty(prefix = "indexing", name = ["consumer"], havingValue = "sqs")
class SqsGroupBuyIndexingEventConsumer(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    private val properties: IndexingProperties,
    private val indexingService: GroupBuyIndexingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${indexing.sqs.polling-fixed-delay-millis:5000}")
    fun poll() {
        val queueUrl = properties.sqs.queueUrl
        if (queueUrl.isBlank()) {
            log.warn("SQS 인덱싱 이벤트 수신 생략: queueUrl 미설정")
            return
        }

        val messages = sqsClient.receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(properties.sqs.maxMessages.coerceIn(1, 10))
                .waitTimeSeconds(properties.sqs.waitTimeSeconds.coerceIn(0, 20))
                .visibilityTimeout(properties.sqs.visibilityTimeoutSeconds)
                .build()
        ).messages()

        messages.forEach { message ->
            try {
                val event = objectMapper.readValue(message.body(), GroupBuyIndexRequestedEvent::class.java)
                val processed = indexingService.process(event)
                if (processed) {
                    deleteMessage(queueUrl, message.receiptHandle())
                }
            } catch (e: Exception) {
                log.warn("SQS 인덱싱 이벤트 처리 실패, 재시도 예정: messageId={}, error={}", message.messageId(), e.message)
            }
        }
    }

    private fun deleteMessage(queueUrl: String, receiptHandle: String) {
        sqsClient.deleteMessage(
            DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(receiptHandle)
                .build()
        )
    }
}
