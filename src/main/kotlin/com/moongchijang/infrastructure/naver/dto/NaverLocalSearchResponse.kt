package com.moongchijang.infrastructure.naver.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class NaverLocalSearchResponse(
    val total: Int,
    val start: Int,
    val display: Int,
    val items: List<NaverLocalSearchItem>
)

data class NaverLocalSearchItem(
    val title: String,
    val link: String,
    val category: String,
    val description: String,
    val telephone: String,
    val address: String,
    @JsonProperty("roadAddress")
    val roadAddress: String,
    val mapx: String,
    val mapy: String
) {
    fun placeId(): String = link.trimEnd('/').substringAfterLast('/')

    fun storeName(): String = title.replace(Regex("<[^>]+>"), "")

    fun latitude(): Double = mapy.toDouble() / 1e7

    fun longitude(): Double = mapx.toDouble() / 1e7
}
