package com.moongchijang.domain.notification.infrastructure.aligo

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Component
class AligoAlimtalkClient(
    private val restClient: RestClient,
    private val aligoProperties: AligoProperties
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val sendUrl = "https://kakaoapi.aligo.in/akv10/alimtalk/send/"

    fun send(receiverPhone: String, message: String): Boolean {
        return try {
            val body = LinkedMultiValueMap<String, String>().apply {
                add("apikey", aligoProperties.apiKey)
                add("userid", aligoProperties.userId)
                add("senderkey", aligoProperties.senderKey)
                add("tpl_code", aligoProperties.templateCode)
                add("sender", aligoProperties.sender)
                add("receiver_1", receiverPhone)
                add("subject_1", "공구 알림")
                add("message_1", message)
            }

            val response = restClient.post()
                .uri(sendUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(String::class.java)

            log.info("알리고 알림톡 발송 성공: receiver={}, response={}", receiverPhone, response)
            true
        } catch (e: Exception) {
            log.error("알리고 알림톡 발송 실패: receiver={}, error={}", receiverPhone, e.message)
            false
        }
    }
}
