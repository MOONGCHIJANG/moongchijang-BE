package com.moongchijang.domain.user.application

import com.moongchijang.domain.user.domain.entity.User
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.security.crypto.PersonalInfoManager
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class PersonalInfoBackfillService(
    private val userRepository: UserRepository,
    private val personalInfoManager: PersonalInfoManager,
    transactionManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun backfill(batchSize: Int): Int {
        require(batchSize > 0) { "batchSize must be positive" }

        var totalUpdated = 0
        var lastProcessedId = 0L

        while (true) {
            val users = userRepository.findPersonalInfoBackfillTargets(
                lastProcessedId,
                PageRequest.of(0, batchSize),
            )
            if (users.isEmpty()) {
                break
            }

            val updatedInBatch = transactionTemplate.execute { status ->
                runCatching {
                    users.forEach { user ->
                        if (backfillUser(user)) {
                            totalUpdated += 1
                        }
                    }

                    userRepository.saveAll(users)
                    userRepository.flush()
                    users.count(::isBackfilled)
                }.getOrElse { throwable ->
                    status.setRollbackOnly()
                    throw throwable
                }
            } ?: 0

            lastProcessedId = requireNotNull(users.last().id)
            log.info(
                "[PersonalInfoBackfillService] 개인정보 백필 배치 처리 완료: batchSize={}, updatedInBatch={}, totalUpdated={}, lastProcessedId={}",
                users.size,
                updatedInBatch,
                totalUpdated,
                lastProcessedId,
            )
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

    private fun isBackfilled(user: User): Boolean {
        val emailBackfilled = user.email.isNullOrBlank() ||
            (personalInfoManager.isEncrypted(user.email!!) && !user.emailHash.isNullOrBlank())
        val phoneBackfilled = user.phoneNumber.isNullOrBlank() || personalInfoManager.isEncrypted(user.phoneNumber!!)
        return emailBackfilled && phoneBackfilled
    }
}
