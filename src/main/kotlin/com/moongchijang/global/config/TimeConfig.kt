package com.moongchijang.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.auditing.DateTimeProvider
import java.time.Clock
import java.util.Optional
import com.moongchijang.global.time.utcNow

@Configuration
class TimeConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun auditingDateTimeProvider(clock: Clock): DateTimeProvider =
        DateTimeProvider { Optional.of(clock.utcNow()) }
}
