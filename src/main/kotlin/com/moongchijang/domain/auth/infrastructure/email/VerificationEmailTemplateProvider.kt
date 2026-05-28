package com.moongchijang.domain.auth.infrastructure.email

import org.springframework.stereotype.Component

@Component
class VerificationEmailTemplateProvider {

    fun build(code: String, expiresInSeconds: Long): VerificationEmailTemplate {
        val expiresMinutes = expiresInSeconds / 60
        val subject = "[뭉치장] 이메일 인증코드를 확인해주세요"
        val bodyText = """
            안녕하세요, 뭉치장입니다.

            이메일 인증코드는 아래와 같습니다.
            $code

            인증코드는 ${expiresMinutes}분간 유효합니다.
        """.trimIndent()
        val bodyHtml = """
            <!doctype html>
            <html lang="ko">
            <body style="margin:0;padding:24px;background:#f8fafc;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Noto Sans KR',Arial,sans-serif;color:#111827;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:12px;">
                <tr>
                  <td style="padding:28px 24px 12px 24px;font-size:24px;font-weight:700;line-height:1.3;">이메일 인증코드를 확인해주세요</td>
                </tr>
                <tr>
                  <td style="padding:0 24px 20px 24px;font-size:16px;line-height:1.7;">
                    안녕하세요, 뭉치장입니다.<br/>
                    이메일 인증코드는 아래와 같습니다.
                  </td>
                </tr>
                <tr>
                  <td style="padding:0 24px 20px 24px;">
                    <div style="background:#f3f4f6;border:1px solid #d1d5db;border-radius:10px;text-align:center;padding:18px 12px;">
                      <span style="font-size:34px;font-weight:800;letter-spacing:6px;color:#111827;">$code</span>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td style="padding:0 24px 28px 24px;font-size:15px;color:#374151;line-height:1.6;">
                    인증코드는 <b>${expiresMinutes}분</b>간 유효합니다.
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """.trimIndent()

        return VerificationEmailTemplate(
            subject = subject,
            bodyText = bodyText,
            bodyHtml = bodyHtml
        )
    }
}

data class VerificationEmailTemplate(
    val subject: String,
    val bodyText: String,
    val bodyHtml: String,
)
