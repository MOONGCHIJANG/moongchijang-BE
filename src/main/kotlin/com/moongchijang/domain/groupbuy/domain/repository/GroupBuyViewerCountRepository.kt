package com.moongchijang.domain.groupbuy.domain.repository

interface GroupBuyViewerCountRepository {

    fun touch(groupBuyId: Long, viewerKey: String, nowEpochSeconds: Long, ttlSeconds: Long)
    fun countActive(groupBuyId: Long, nowEpochSeconds: Long, ttlSeconds: Long): Long
}
