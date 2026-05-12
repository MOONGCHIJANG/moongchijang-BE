package com.moongchijang.domain.search.application

import com.moongchijang.global.config.SearchIndexProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service

@Service
class SearchIndexVersionService(
    private val redisTemplate: StringRedisTemplate,
    private val properties: SearchIndexProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun currentVersion(): String =
        try {
            redisTemplate.opsForValue().get(properties.versionKey) ?: properties.defaultVersion
        } catch (e: Exception) {
            log.warn("검색 인덱스 버전 조회 실패, 기본 버전 사용: error={}", e.message)
            properties.defaultVersion
        }

    fun bumpVersion(): String =
        try {
            redisTemplate.opsForValue().increment(properties.versionKey)?.toString()
                ?: properties.defaultVersion
        } catch (e: Exception) {
            log.warn("검색 인덱스 버전 증가 실패: error={}", e.message)
            properties.defaultVersion
        }
}
