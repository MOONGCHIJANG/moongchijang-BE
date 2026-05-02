package com.moongchijang.domain.auth.infrastructure.sms.coolsms

import com.moongchijang.global.config.CoolSmsProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException
import com.solapi.sdk.message.model.Message
import com.solapi.sdk.message.service.DefaultMessageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CoolSmsSender(
    private val coolSmsMessageService: DefaultMessageService,
    private val coolSmsProperties: CoolSmsProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun sendVerificationCode(to: String, code: String, expiresInSeconds: Long) {
        val normalizedSender = normalizePhoneNumber(coolSmsProperties.sender)
        val normalizedTo = normalizePhoneNumber(to)
        val validMinutes = (expiresInSeconds / SECONDS_PER_MINUTE).coerceAtLeast(1L)
        val text = "[뭉치장] 인증번호는 [$code]입니다. ${validMinutes}분 내로 입력해주세요."

        val message = Message().apply {
            from = normalizedSender
            this.to = normalizedTo
            this.text = text
        }

        try {
            coolSmsMessageService.send(message)
        } catch (e: SolapiMessageNotReceivedException) {
            log.error(
                "[CoolSmsSender] 문자 발송 실패: to={}, sender={}, reason={}, failed={}",
                maskPhoneNumber(normalizedTo),
                maskPhoneNumber(normalizedSender),
                e.message,
                e.failedMessageList
            )
            throw CustomException(ErrorCode.SMS_SEND_FAILED, "문자 발송 처리 중 실패했습니다.")
        } catch (e: Exception) {
            log.error(
                "[CoolSmsSender] 문자 발송 중 예기치 못한 오류: to={}, sender={}",
                maskPhoneNumber(normalizedTo),
                maskPhoneNumber(normalizedSender),
                e
            )
            throw CustomException(ErrorCode.SMS_SEND_FAILED, "문자 발송 처리 중 예기치 못한 오류가 발생했습니다.")
        }
    }

    private fun normalizePhoneNumber(raw: String): String {
        return raw.replace(Regex("[^0-9]"), "")
    }

    private fun maskPhoneNumber(phoneNumber: String): String {
        if (phoneNumber.length < 7) return "***"
        return phoneNumber.replace(Regex("(\\d{3})\\d+(\\d{4})"), "$1****$2")
    }

    companion object {
        private const val SECONDS_PER_MINUTE = 60L
    }
}
