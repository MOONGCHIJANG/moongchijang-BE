package com.moongchijang.domain.payment.experiment

import com.moongchijang.domain.payment.application.PaymentService
import com.moongchijang.domain.payment.application.dto.CreatePaymentOrderRequest
import com.moongchijang.domain.user.application.UserService
import com.moongchijang.security.jwt.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentExperimentPreparationService(
    private val userService: UserService,
    private val paymentService: PaymentService,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Transactional
    fun prepare(request: PaymentExperimentPreparationRequest): PaymentExperimentPreparationResponse {
        require(request.userCount > 0) { "userCount must be positive" }

        val requests = (1..request.userCount).map { index ->
            val email = buildEmail(
                prefix = request.emailPrefix,
                domain = request.emailDomain,
                index = index,
            )

            val user = userService.findActiveEmailUser(email)
                ?: userService.createEmailUser(email, requireNotNull(passwordEncoder.encode(request.password)))

            val userId = requireNotNull(user.id)
            val accessToken = jwtTokenProvider.generateAccessToken(userId)
            val order = paymentService.createPaymentOrder(
                groupBuyId = request.groupBuyId,
                userId = userId,
                request = CreatePaymentOrderRequest(
                    quantity = request.quantityPerOrder,
                    agreedNoCancelAfterGoal = true,
                    agreedRefundBeforeGoal = true,
                    agreedNoRefundAfterNoShow = true,
                    agreedNoWithdrawal = true,
                ),
            )

            PreparedPaymentRequestPayload(
                userId = userId,
                email = email,
                accessToken = accessToken,
                paymentId = order.paymentId,
                amount = order.amount,
            )
        }

        return PaymentExperimentPreparationResponse(requests = requests)
    }

    private fun buildEmail(prefix: String, domain: String, index: Int): String {
        return "%s-%03d@%s".format(prefix, index, domain)
    }
}
