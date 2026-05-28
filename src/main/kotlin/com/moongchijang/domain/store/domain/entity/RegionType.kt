package com.moongchijang.domain.store.domain.entity

import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode

enum class RegionType(val label: String) {
    NATIONWIDE("전국"),
    SEOUL("서울"),
    GYEONGGI("경기"),
    INCHEON("인천"),
    GANGWON("강원"),
    DAEJEON("대전"),
    SEJONG("세종"),
    CHUNGNAM("충남"),
    CHUNGBUK("충북"),
    BUSAN("부산"),
    ULSAN("울산"),
    GYEONGNAM("경남"),
    GYEONGBUK("경북"),
    DAEGU("대구"),
    GWANGJU("광주"),
    JEONNAM("전남"),
    JEONBUK("전북"),
    JEJU("제주");

    companion object {
        private val BY_LABEL = entries.associateBy { it.label }

        fun fromLabel(label: String): RegionType =
            BY_LABEL[label.trim()]
                ?: throw CustomException(ErrorCode.INVALID_REGION_TYPE_LABEL, "label=$label")
    }
}
