package com.moongchijang.infrastructure.sms.coolsms.config

import com.moongchijang.global.config.CoolSmsProperties
import com.solapi.sdk.SolapiClient
import com.solapi.sdk.message.service.DefaultMessageService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CoolSmsConfig(
    private val coolSmsProperties: CoolSmsProperties,
) {
    @Bean
    fun coolSmsMessageService(): DefaultMessageService {
        return SolapiClient.createInstance(
            coolSmsProperties.apiKey,
            coolSmsProperties.apiSecret
        )
    }
}
