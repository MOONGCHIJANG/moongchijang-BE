package com.moongchijang.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner

@Configuration
class S3Config(
    private val appS3Properties: AppS3Properties,
) {
    @Bean
    fun s3Client(): S3Client =
        S3Client.builder()
            .region(Region.of(appS3Properties.region))
            .build()

    @Bean(destroyMethod = "close")
    fun s3Presigner(): S3Presigner =
        S3Presigner.builder()
            .region(Region.of(appS3Properties.region))
            .build()
}
