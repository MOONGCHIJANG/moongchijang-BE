package com.moongchijang.domain.notification.infrastructure.aligo

object AligoMessageFormatter {
    fun groupBuyOpenFailed(
        nickname: String,
        productName: String,
        pickupPlace: String,
        pickupDate: String,
    ): String {
        return """
            ${nickname}님, 공구 요청 심사 결과가 나왔어요.

            요청하신 공구를 개설하기 위해 뭉치장이 꼼꼼히 확인했어요.
            심사결과를 안내드려요.

            <요청하신 내역>
            - 상품명: ${productName}
            - 픽업 장소: ${pickupPlace}
            - 픽업 일시: ${pickupDate}

            ※ 뭉치장은 늘 행복한 디저트를 웨이팅없이 맛보실 수 있도록 최선을 다하겠습니다.

            - 팀 뭉치장 드림
        """.trimIndent()
    }

    fun pickupD1Reminder(
        nickname: String,
        productName: String,
        pickupPlace: String,
        pickupDateTime: String,
    ): String {
        return """
            ${nickname}님, 내일 기다리던 픽업 날이에요!

            - 상품명: ${productName}
            - 픽업 장소: ${pickupPlace}
            - 픽업 일시: ${pickupDateTime}

            QR 픽업 코드는 내일 00시에
            뭉치장 앱에서 자동 발급됩니다.

            ※ 픽업 미수령 시 환불이 불가하오니
            일정을 꼭 확인해 주세요.

            - 팀 뭉치장 드림
        """.trimIndent()
    }

    fun pickupDayReminder(
        nickname: String,
        productName: String,
        pickupPlace: String,
        pickupDateTime: String,
    ): String {
        return """
            ${nickname}님, 오늘 픽업 당일이에요!

            - 상품명: ${productName}
            - 픽업 장소: ${pickupPlace}
            - 픽업 일시: ${pickupDateTime}

            자정부터 QR 픽업 코드가 발급되어 있어요.
            뭉치장 웹(www.moongchijang.com)에서 확인 후 매장을 방문해 주세요.

            ※ 미수령 시 환불이 불가합니다.

            언제나 감사드려요 ♥️
            늘 노력하는 뭉치장 되겠습니다.

            - 팀 뭉치장 드림
        """.trimIndent()
    }
}
