package com.moongchijang.security.crypto

import org.springframework.stereotype.Component
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class HmacSha256PersonalInfoHasher(
    properties: PersonalInfoEncryptionProperties,
) : PersonalInfoHasher {

    private val secretKey = SecretKeySpec(
        Base64.getDecoder().decode(properties.secretKey),
        ALGORITHM,
    )

    override fun hash(value: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(secretKey)
        return mac.doFinal(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val ALGORITHM = "HmacSHA256"
    }
}
