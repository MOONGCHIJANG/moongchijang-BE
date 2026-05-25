package com.moongchijang.domain.user.application

object SettlementInstitutionCodeMapper {
    private val aliasToCode: Map<String, String> = buildMap {
        fun add(code: String, vararg aliases: String) {
            put(normalizeKey(code), code)
            aliases.forEach { put(normalizeKey(it), code) }
        }

        add("NONGHYEOP", "NH농협", "농협", "NH농협은행")
        add("KAKAOBANK", "카카오뱅크", "카카오")
        add("KOOKMIN", "KB국민", "국민", "KB국민은행")
        add("TOSSBANK", "토스뱅크")
        add("SHINHAN", "신한", "신한은행")
        add("WOORI", "우리", "우리은행")
        add("IBK", "IBK기업", "기업", "IBK기업은행")
        add("HANA", "하나", "하나은행")
        add("SAEMAUL", "새마을", "새마을금고")
        add("BUSANBANK", "부산", "부산은행")
        add("DAEGUBANK", "iM뱅크(대구)", "iM뱅크", "대구", "대구은행")
        add("KBANK", "케이뱅크", "케이")
        add("SHINHYEOP", "신협")
        add("POST", "우체국", "우체국예금보험")
        add("SC", "SC제일", "SC제일은행")
        add("KYONGNAMBANK", "경남", "경남은행")
        add("GWANGJUBANK", "광주", "광주은행")
        add("SUHYEOP", "수협", "Sh수협", "Sh수협은행")
        add("JEONBUKBANK", "전북", "전북은행")
        add("SAVINGBANK", "저축은행", "저축은행중앙회", "SBI저축은행")
        add("JEJUBANK", "제주", "제주은행")
        add("CITI", "씨티", "씨티은행")
        add("KDBBANK", "KDB산업", "산업", "한국산업은행")
        add("SANLIM", "산림조합")
        add("BOA", "Bank of America")
        add("HSBC", "홍콩상하이은행")

        add("SAMSUNG_SECURITIES", "삼성증권")
        add("KB_SECURITIES", "KB증권")
        add("NH_INVESTMENT_AND_SECURITIES", "NH투자", "NH투자증권")
        add("YUANTA_SECURITES", "유안타", "유안타증권")
        add("DAISHIN_SECURITIES", "대신", "대신증권")
        add("HANA_INVESTMENT_AND_SECURITIES", "하나증권", "하나금융투자")
        add("HANHWA_INVESTMENT_AND_SECURITIES", "한화투자", "한화투자증권")
        add("EUGENE_INVESTMENT_AND_SECURITIES", "유진투자", "유진투자증권")
        add("HI_INVESTMENT_AND_SECURITIES", "아이엠증권", "하이투자증권")
        add("KYOBO_SECURITIES", "교보", "교보증권")
        add("MERITZ_SECURITIES", "메리츠증권")
        add("SK_SECURITIES", "SK", "SK증권")
        add("LIG_INVESTMENT_AND_SECURITIES", "LS", "LIG투자")
        add("HYUNDAI_MOTOR_SECURITIES", "현대차증권")
        add("DB_INVESTMENT_AND_SECURITIES", "DB증권", "DB금융투자")
        add("SHINYOUNG_SECURITIES", "신영", "신영증권")
        add("DAOL_INVESTMENT_AND_SECURITIES", "다올투자증권", "KTB투자증권")
        add("BOOKOOK_SECURITIES", "부국", "부국증권")
        add("TOSS_SECURITIES", "토스증권")
        add("KAKAOPAY_SECURITIES", "카카오페이증권")
        add("MIRAE_ASSET_SECURITIES", "미래에셋", "미래에셋증권")
        add("KIWOOM", "키움", "키움증권")
        add("KOREA_INVESTMENT_AND_SECURITIES", "한국투자", "한국투자증권")
        add("SHINHAN_SECURITIES", "신한투자", "신한금융투자", "신한투자증권")

        add("IBK_INVESTMENT_SECURITIES", "IBK투자", "IBK투자증권")
        add("WOORI_SECURITIES", "우리투자증권")
        add("CAPE_INVESTMENT_SECURITIES", "케이프투자", "케이프투자증권")
        add("BNK_INVESTMENT_SECURITIES", "BNK투자", "BNK투자증권")
        add("SANGSANGIN_SECURITIES", "상상인증권")
        add("ICBC", "중국공상", "중국공상은행")
        add("DEUTSCHE_BANK", "도이치", "도이치은행")
        add("JPMORGAN_CHASE", "JP모건", "JP모건체이스")
        add("BNP_PARIBAS", "BNP파리바")
        add("CCB", "중국건설", "중국건설은행")
        add("BOC", "중국", "중국은행")
    }

    fun toCode(raw: String): String {
        val normalizedKey = normalizeKey(raw)
        return aliasToCode[normalizedKey] ?: toCustomCode(raw)
    }

    private fun normalizeKey(raw: String): String {
        return raw.trim()
            .replace(Regex("[\\s\\-_/().]+"), "")
            .uppercase()
    }

    private fun toCustomCode(raw: String): String {
        val body = raw.trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "UNKNOWN" }
        return if (body.first().isDigit()) "CUSTOM_$body" else body
    }
}
