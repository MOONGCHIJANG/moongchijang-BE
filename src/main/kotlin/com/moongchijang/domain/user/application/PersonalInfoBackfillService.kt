package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.security.crypto.PersonalInfoManager
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonalInfoBackfillService(
    private val userRepository: UserRepository,
    private val personalInfoManager: PersonalInfoManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun backfill(batchSize: Int): Int {
        require(batchSize > 0) { "batchSize must be positive" }

        var totalUpdated = 0

        while (true) {
            val users = userRepository.findPersonalInfoBackfillTargets(PageRequest.of(0, batchSize))
            if (users.isEmpty()) {
                break
            }

            users.forEach { user ->
                if (backfillUser(user)) {
                    totalUpdated += 1
                }
            }

            userRepository.saveAll(users)
            userRepository.flush()
            log.info("[PersonalInfoBackfillService] 개인정보 백필 배치 처리 완료: batchSize={}, totalUpdated={}", users.size, totalUpdated)
        }

        return totalUpdated
    }

    internal fun backfillUser(user: User): Boolean {
        var updated = false

        val currentEmail = user.email?.trim()
        if (!currentEmail.isNullOrBlank() && !personalInfoManager.isEncrypted(currentEmail)) {
            val normalizedEmail = currentEmail.lowercase()
            user.email = personalInfoManager.encryptEmail(normalizedEmail)
            if (user.emailHash.isNullOrBlank()) {
                user.emailHash = personalInfoManager.hashEmail(normalizedEmail)
            }
            updated = true
        } else if (!currentEmail.isNullOrBlank() && user.emailHash.isNullOrBlank()) {
            val decryptedEmail = personalInfoManager.decryptIfNeeded(currentEmail)?.trim()?.lowercase()
            if (!decryptedEmail.isNullOrBlank()) {
                user.emailHash = personalInfoManager.hashEmail(decryptedEmail)
                updated = true
            }
        }

        val currentPhoneNumber = user.phoneNumber?.trim()
        if (!currentPhoneNumber.isNullOrBlank() && !personalInfoManager.isEncrypted(currentPhoneNumber)) {
            user.phoneNumber = personalInfoManager.encryptPhone(currentPhoneNumber)
            updated = true
        }

        return updated
    }
}
