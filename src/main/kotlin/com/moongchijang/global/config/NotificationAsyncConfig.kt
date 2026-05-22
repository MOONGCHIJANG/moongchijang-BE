package com.moongchijang.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
class NotificationAsyncConfig {

    @Bean(name = ["notificationEventExecutor"])
    fun notificationEventExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 4
            maxPoolSize = 8
            queueCapacity = 500
            setThreadNamePrefix("notification-event-")
            initialize()
        }
    }
}
