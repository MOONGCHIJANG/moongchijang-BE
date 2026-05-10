package com.moongchijang.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sesv2.SesV2Client

@Configuration
class SesConfig(
    private val sesProperties: SesProperties,
) {

    @Bean(destroyMethod = "close")
    fun sesV2Client(): SesV2Client = SesV2Client.builder()
        .region(Region.of(sesProperties.region))
        .build()
}
