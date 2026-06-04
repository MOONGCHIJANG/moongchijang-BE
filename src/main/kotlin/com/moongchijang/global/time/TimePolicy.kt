package com.moongchijang.global.time

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

object TimePolicy {
    val STORAGE_ZONE_ID: ZoneId = ZoneOffset.UTC
    val BUSINESS_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
    const val BUSINESS_TIME_ZONE_ID: String = "Asia/Seoul"
}

fun Clock.kstNow(): LocalDateTime = LocalDateTime.ofInstant(instant(), TimePolicy.BUSINESS_ZONE_ID)

fun Clock.kstToday(): LocalDate = instant().atZone(TimePolicy.BUSINESS_ZONE_ID).toLocalDate()

fun Clock.utcNow(): LocalDateTime = LocalDateTime.ofInstant(instant(), TimePolicy.STORAGE_ZONE_ID)
