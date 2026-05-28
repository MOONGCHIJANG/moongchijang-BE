package com.moongchijang.domain.store.domain.entity

import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode

enum class DistrictType(
    val region: RegionType,
    val label: String
) {
    NATIONWIDE(RegionType.NATIONWIDE, "전국"),

    SEOUL_ALL(RegionType.SEOUL, "서울 전체"),
    SEOUL_GANGNAM_YEOKSAM_SAMSEONG(RegionType.SEOUL, "강남 | 역삼 | 삼성"),
    SEOUL_SINSA_APGUJEONG_CHEONGDAM(RegionType.SEOUL, "신사 | 압구정 | 청담"),
    SEOUL_SEOCHO_BANGBAE_GYODAE(RegionType.SEOUL, "서초 | 방배 | 교대"),
    SEOUL_JAMSIL_SINCHEON_SONGPA(RegionType.SEOUL, "잠실 | 신천 | 송파"),
    SEOUL_JONGNO_JUNGGU_EULJIRO_MYEONGDONG(RegionType.SEOUL, "종로 | 중구 | 을지로 | 명동"),
    SEOUL_HONGDAE_HAPJEONG_SANGSU_MAPO(RegionType.SEOUL, "홍대 | 합정 | 상수 | 마포"),
    SEOUL_SEONGSU_GEONDAE_GWANGJIN(RegionType.SEOUL, "성수 | 건대 | 광진"),
    SEOUL_ITAEWON_HANNAM_YONGSAN(RegionType.SEOUL, "이태원 | 한남 | 용산"),
    SEOUL_YEONGDEUNGPO_YEOUIDO(RegionType.SEOUL, "영등포 | 여의도"),
    SEOUL_NOWON_DOBONG_GANGBUK(RegionType.SEOUL, "노원 | 도봉 | 강북"),
    SEOUL_ETC(RegionType.SEOUL, "기타 서울 지역"),

    GYEONGGI_ALL(RegionType.GYEONGGI, "경기 전체"),
    GYEONGGI_SUWON_YEONGTONG_PALDAL(RegionType.GYEONGGI, "수원 | 영통 | 팔달"),
    GYEONGGI_SEONGNAM_BUNDANG_PANGYO(RegionType.GYEONGGI, "성남 | 분당 | 판교"),
    GYEONGGI_GOYANG_ILSAN(RegionType.GYEONGGI, "고양 | 일산"),
    GYEONGGI_YONGIN_SUJI_GIHEUNG(RegionType.GYEONGGI, "용인 | 수지 | 기흥"),
    GYEONGGI_BUCHEON_JUNGDONG_SANGDONG(RegionType.GYEONGGI, "부천 | 중동 | 상동"),
    GYEONGGI_ANSAN_DANWON_SANGROK(RegionType.GYEONGGI, "안산 | 단원 | 상록"),
    GYEONGGI_NAMYANGJU_DASAN_BYEOLNAE(RegionType.GYEONGGI, "남양주 | 다산 | 별내"),
    GYEONGGI_ANYANG_PYEONGCHON_BEOMGYE(RegionType.GYEONGGI, "안양 | 평촌 | 범계"),
    GYEONGGI_HWASEONG_DONGTAN(RegionType.GYEONGGI, "화성 | 동탄"),
    GYEONGGI_PAJU_UNJEONG(RegionType.GYEONGGI, "파주 | 운정"),
    GYEONGGI_ETC(RegionType.GYEONGGI, "기타 경기 지역"),

    INCHEON_ALL(RegionType.INCHEON, "인천 전체"),
    INCHEON_SONGDO_YEONSU(RegionType.INCHEON, "송도 | 연수"),
    INCHEON_GUWOL_NAMDONG(RegionType.INCHEON, "구월 | 남동"),
    INCHEON_BUPYEONG_GYEYANG(RegionType.INCHEON, "부평 | 계양"),
    INCHEON_CHEONGNA_SEOGU(RegionType.INCHEON, "청라 | 서구"),
    INCHEON_JUAN_MICHUHOL(RegionType.INCHEON, "주안 | 미추홀"),
    INCHEON_YEONGJONGDO_JUNGGU(RegionType.INCHEON, "영종도 | 중구"),

    GANGWON_ALL(RegionType.GANGWON, "강원 전체"),
    GANGWON_CHUNCHEON(RegionType.GANGWON, "춘천"),
    GANGWON_WONJU(RegionType.GANGWON, "원주"),
    GANGWON_GANGNEUNG(RegionType.GANGWON, "강릉"),
    GANGWON_SOKCHO_YANGYANG(RegionType.GANGWON, "속초 | 양양"),
    GANGWON_DONGHAE_SAMCHEOK(RegionType.GANGWON, "동해 | 삼척"),
    GANGWON_ETC(RegionType.GANGWON, "기타 강원 지역"),

    DAEJEON_ALL(RegionType.DAEJEON, "대전 전체"),
    DAEJEON_DUNSAN_SEOGU(RegionType.DAEJEON, "둔산 | 서구"),
    DAEJEON_EUNHAENG_DAEHEUNG_JUNGGU(RegionType.DAEJEON, "은행 | 대흥 | 중구"),
    DAEJEON_YUSEONG_DOAN(RegionType.DAEJEON, "유성 | 도안"),
    DAEJEON_DONGGU(RegionType.DAEJEON, "동구"),
    DAEJEON_DAEDEOK(RegionType.DAEJEON, "대덕"),

    SEJONG_ALL(RegionType.SEJONG, "세종 전체"),

    CHUNGNAM_ALL(RegionType.CHUNGNAM, "충남 전체"),
    CHUNGNAM_CHEONAN_SINBU_DUJEONG(RegionType.CHUNGNAM, "천안 | 신부 | 두정"),
    CHUNGNAM_ASAN_TANGJEONG(RegionType.CHUNGNAM, "아산 | 탕정"),
    CHUNGNAM_DANGJIN(RegionType.CHUNGNAM, "당진"),
    CHUNGNAM_SEOSAN(RegionType.CHUNGNAM, "서산"),
    CHUNGNAM_GYERYONG_NONSAN(RegionType.CHUNGNAM, "계룡 | 논산"),
    CHUNGNAM_ETC(RegionType.CHUNGNAM, "기타 충남 지역"),

    CHUNGBUK_ALL(RegionType.CHUNGBUK, "충북 전체"),
    CHUNGBUK_CHEONGJU_SANGDANG_HEUNGDEOK(RegionType.CHUNGBUK, "청주 | 상당 | 흥덕"),
    CHUNGBUK_CHUNGJU(RegionType.CHUNGBUK, "충주"),
    CHUNGBUK_JECHEON(RegionType.CHUNGBUK, "제천"),
    CHUNGBUK_EUMSEONG_JINCHEON(RegionType.CHUNGBUK, "음성 | 진천"),
    CHUNGBUK_ETC(RegionType.CHUNGBUK, "기타 충북 지역"),

    BUSAN_ALL(RegionType.BUSAN, "부산 전체"),
    BUSAN_SEOMYEON_JEONPO_JINGU(RegionType.BUSAN, "서면 | 전포 | 진구"),
    BUSAN_HAEUNDAE_CENTUM_MARINE_CITY(RegionType.BUSAN, "해운대 | 센텀 | 마린시티"),
    BUSAN_GWANGALLI_SUYEONG_NAMCHEON(RegionType.BUSAN, "광안리 | 수영 | 남천"),
    BUSAN_NAMPO_JUNGGU_YEONGDO(RegionType.BUSAN, "남포 | 중구 | 영도"),
    BUSAN_DONGRAE_YEONSAN_BUSANDAE(RegionType.BUSAN, "동래 | 연산 | 부산대"),
    BUSAN_SAHA_HADAN(RegionType.BUSAN, "사하 | 하단"),
    BUSAN_ETC(RegionType.BUSAN, "기타 부산 지역"),

    ULSAN_ALL(RegionType.ULSAN, "울산 전체"),
    ULSAN_SAMSAN_DALDONG_NAMGU(RegionType.ULSAN, "삼산 | 달동 | 남구"),
    ULSAN_SEONGNAM_JUNGGU(RegionType.ULSAN, "성남 | 중구"),
    ULSAN_DONGGU(RegionType.ULSAN, "동구"),
    ULSAN_BUKGU(RegionType.ULSAN, "북구"),
    ULSAN_ULJU(RegionType.ULSAN, "울주"),

    GYEONGNAM_ALL(RegionType.GYEONGNAM, "경남 전체"),
    GYEONGNAM_CHANGWON_SANGNAM_UICHANG(RegionType.GYEONGNAM, "창원 | 상남 | 의창"),
    GYEONGNAM_GIMHAE(RegionType.GYEONGNAM, "김해"),
    GYEONGNAM_YANGSAN(RegionType.GYEONGNAM, "양산"),
    GYEONGNAM_JINJU(RegionType.GYEONGNAM, "진주"),
    GYEONGNAM_GEOJE_TONGYEONG(RegionType.GYEONGNAM, "거제 | 통영"),
    GYEONGNAM_ETC(RegionType.GYEONGNAM, "기타 경남 지역"),

    GYEONGBUK_ALL(RegionType.GYEONGBUK, "경북 전체"),
    GYEONGBUK_POHANG(RegionType.GYEONGBUK, "포항"),
    GYEONGBUK_GYEONGJU_HWANGRIDAN_GIL(RegionType.GYEONGBUK, "경주 | 황리단길"),
    GYEONGBUK_GUMI(RegionType.GYEONGBUK, "구미"),
    GYEONGBUK_GYEONGSAN(RegionType.GYEONGBUK, "경산"),
    GYEONGBUK_ANDONG(RegionType.GYEONGBUK, "안동"),
    GYEONGBUK_ETC(RegionType.GYEONGBUK, "기타 경북 지역"),

    DAEGU_ALL(RegionType.DAEGU, "대구 전체"),
    DAEGU_DONGSEONGNO_JUNGGU(RegionType.DAEGU, "동성로 | 중구"),
    DAEGU_SUSEONGGU_BEOMEO(RegionType.DAEGU, "수성구 | 범어"),
    DAEGU_SANGIN_DALSEO(RegionType.DAEGU, "상인 | 달서"),
    DAEGU_CHILGOK_BUKGU(RegionType.DAEGU, "칠곡 | 북구"),
    DAEGU_DONGGU(RegionType.DAEGU, "동구"),
    DAEGU_ETC(RegionType.DAEGU, "기타 대구 지역"),

    GWANGJU_ALL(RegionType.GWANGJU, "광주 전체"),
    GWANGJU_SANGMU_JIGYEONG_SEOGU(RegionType.GWANGJU, "상무지구 | 치평 | 서구"),
    GWANGJU_DONGMYEONGDONG_CHUNGJANGRO_DONGGU(RegionType.GWANGJU, "동명동 | 충장로 | 동구"),
    GWANGJU_SUWAN_CHEOMDAN_GWANGSANGU(RegionType.GWANGJU, "수완 | 첨단 | 광산구"),
    GWANGJU_BONGSEON_NAMGU(RegionType.GWANGJU, "봉선 | 남구"),
    GWANGJU_BUKGU(RegionType.GWANGJU, "북구"),

    JEONNAM_ALL(RegionType.JEONNAM, "전남 전체"),
    JEONNAM_YEOSU(RegionType.JEONNAM, "여수"),
    JEONNAM_SUNCHEON(RegionType.JEONNAM, "순천"),
    JEONNAM_MOKPO_NAMAK(RegionType.JEONNAM, "목포 | 남악"),
    JEONNAM_NAJU(RegionType.JEONNAM, "나주"),
    JEONNAM_ETC(RegionType.JEONNAM, "기타 전남 지역"),

    JEONBUK_ALL(RegionType.JEONBUK, "전북 전체"),
    JEONBUK_JEONJU_GAEKRIDANGIL_WANSAN(RegionType.JEONBUK, "전주 | 객리단길 | 완산"),
    JEONBUK_IKSAN(RegionType.JEONBUK, "익산"),
    JEONBUK_GUNSAN(RegionType.JEONBUK, "군산"),
    JEONBUK_ETC(RegionType.JEONBUK, "기타 전북 지역"),

    JEJU_ALL(RegionType.JEJU, "제주 전체"),
    JEJU_JEJU_SI(RegionType.JEJU, "제주시"),
    JEJU_AEWOL_HALLIM(RegionType.JEJU, "애월 | 한림"),
    JEJU_JOCHEON_GUJWA(RegionType.JEJU, "조천 | 구좌"),
    JEJU_SEOGWIPO_SI(RegionType.JEJU, "서귀포시"),
    JEJU_JUNGMUN_ANDEOK(RegionType.JEJU, "중문 | 안덕"),
    JEJU_SEONGSAN_PYOSEON(RegionType.JEJU, "성산 | 표선");

    companion object {
        private val BY_LABEL = entries.associateBy { it.label }
        private val BY_REGION_AND_LABEL = entries.associateBy { it.region to it.label }

        fun fromLabel(label: String): DistrictType =
            BY_LABEL[label.trim()]
                ?: throw CustomException(ErrorCode.INVALID_DISTRICT_TYPE_LABEL, "label=$label")

        fun from(region: RegionType, label: String): DistrictType =
            BY_REGION_AND_LABEL[region to label.trim()]
                ?: throw CustomException(
                    ErrorCode.INVALID_DISTRICT_TYPE_LABEL,
                    "region=$region, label=$label"
                )

        fun findByRegion(region: RegionType): List<DistrictType> =
            entries.filter { it.region == region }

        fun findLeafByRegion(region: RegionType): List<DistrictType> =
            findByRegion(region).filterNot { it.name.endsWith("_ALL") }
    }
}
