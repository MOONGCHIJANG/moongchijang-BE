package com.moongchijang.support

import com.moongchijang.domain.store.infrastructure.naver.dto.NaverLocalSearchItem

object NaverFixture {
    fun naverItem(
        title: String,
        link: String,
        category: String,
        address: String,
        roadAddress: String
    ): NaverLocalSearchItem {
        return NaverLocalSearchItem(
            title = title,
            link = link,
            category = category,
            description = "",
            telephone = "",
            address = address,
            roadAddress = roadAddress,
            mapx = "1270000000",
            mapy = "375000000"
        )
    }
}
