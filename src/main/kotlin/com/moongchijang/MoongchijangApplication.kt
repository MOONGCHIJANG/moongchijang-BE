package com.moongchijang

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EnableAsync
@EnableScheduling
class MoongchijangApplication

fun main(args: Array<String>) {
    runApplication<MoongchijangApplication>(*args)
}
