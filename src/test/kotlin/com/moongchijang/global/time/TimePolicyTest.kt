package com.moongchijang.global.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class TimePolicyTest {

    @Test
    fun `UTC clock instant를 KST 비즈니스 시각으로 해석한다`() {
        val clock = Clock.fixed(Instant.parse("2026-06-05T00:30:00Z"), ZoneOffset.UTC)

        assertThat(clock.kstNow()).isEqualTo(LocalDateTime.of(2026, 6, 5, 9, 30))
        assertThat(clock.kstToday()).isEqualTo(LocalDate.of(2026, 6, 5))
        assertThat(clock.utcNow()).isEqualTo(LocalDateTime.of(2026, 6, 5, 0, 30))
    }

    @Test
    fun `UTC와 KST 날짜 경계가 월초로 넘어가도 올바르게 해석한다`() {
        val clock = Clock.fixed(Instant.parse("2026-01-31T15:30:00Z"), ZoneOffset.UTC)

        assertThat(clock.utcNow()).isEqualTo(LocalDateTime.of(2026, 1, 31, 15, 30))
        assertThat(clock.kstNow()).isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 30))
        assertThat(clock.kstToday()).isEqualTo(LocalDate.of(2026, 2, 1))
    }
}
