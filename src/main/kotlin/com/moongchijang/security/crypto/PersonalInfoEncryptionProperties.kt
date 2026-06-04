package com.moongchijang.security.crypto

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "security.personal-info")
data class PersonalInfoEncryptionProperties(
    val enabled: Boolean = true,
    @field:NotBlank
    val secretKey: String,
)
