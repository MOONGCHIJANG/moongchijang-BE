package com.moongchijang.domain.groupbuy.domain.repository

interface GroupBuyViewerCountRepository {
    fun touchAndCount(groupBuyId: Long, viewerKey: String, nowEpochSeconds: Long, ttlSeconds: Long): Long
    fun countActive(groupBuyId: Long, nowEpochSeconds: Long, ttlSeconds: Long): Long
}
