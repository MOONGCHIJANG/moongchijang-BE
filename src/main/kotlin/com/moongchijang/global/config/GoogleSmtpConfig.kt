package com.moongchijang.global.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.util.Properties

@Configuration
@ConditionalOnProperty(prefix = "email", name = ["provider"], havingValue = "GOOGLE")
class GoogleSmtpConfig(
    private val googleSmtpProperties: GoogleSmtpProperties,
) {

    @Bean
    fun googleJavaMailSender(): JavaMailSender {
        val sender = JavaMailSenderImpl()
        sender.host = googleSmtpProperties.host
        sender.port = googleSmtpProperties.port
        sender.username = googleSmtpProperties.username
        sender.password = googleSmtpProperties.appPassword
        sender.defaultEncoding = "UTF-8"

        val props = Properties()
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.connectiontimeout"] = "5000"
        props["mail.smtp.timeout"] = "5000"
        props["mail.smtp.writetimeout"] = "5000"
        sender.javaMailProperties = props
        return sender
    }
}
