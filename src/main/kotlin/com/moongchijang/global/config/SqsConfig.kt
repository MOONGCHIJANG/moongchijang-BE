package com.moongchijang.global.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.sqs.SqsClient

@Configuration
@ConditionalOnExpression("'\${indexing.publisher:none}' == 'sqs' or '\${indexing.consumer:none}' == 'sqs'")
class SqsConfig {

    @Bean(destroyMethod = "close")
    fun sqsClient(): SqsClient = SqsClient.create()
}
