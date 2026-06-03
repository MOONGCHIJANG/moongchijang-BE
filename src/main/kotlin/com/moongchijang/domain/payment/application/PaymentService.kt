package com.moongchijang.domain.payment.application

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuy
import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import com.moongchijang.domain.groupbuy.domain.repository.GroupBuyRepository
import com.moongchijang.domain.groupbuy.infrastructure.lock.RedisLockUtil
import com.moongchijang.domain.groupbuy.application.dto.GroupBuyProgressCalculator
import com.moongchijang.domain.favorite.domain.repository.FavoriteRepository
import com.moongchijang.domain.notification.application.discord.AdminDiscordAlertService
import com.moongchijang.domain.notification.application.NotificationEventPublisher
import com.moongchijang.domain.participation.domain.entity.Participation
import com.moongchijang.domain.participation.domain.entity.ParticipationCancelReason
import com.moongchijang.domain.participation.domain.entity.OwnerRefundReviewStatus
import com.moongchijang.domain.participation.domain.entity.ParticipationStatus
import com.moongchijang.domain.participation.domain.repository.ParticipationRepository
import com.moongchijang.domain.payment.application.dto.CancelParticipationRequest
import com.moongchijang.domain.payment.application.dto.CancelParticipationResponse
import com.moongchijang.domain.payment.application.dto.CheckoutInfoResponse
import com.moongchijang.domain.payment.application.dto.CompletePortOnePaymentRequest
import com.moongchijang.domain.payment.application.dto.ConfirmPaymentResponse
import com.moongchijang.domain.payment.application.dto.CreatePaymentOrderRequest
import com.moongchijang.domain.payment.application.dto.CreatePaymentOrderResponse
import com.moongchijang.domain.payment.application.dto.PortOneWebhookRequest
import com.moongchijang.domain.payment.application.port.PortOnePaymentPort
import com.moongchijang.domain.payment.application.port.PortOnePaymentResult
import com.moongchijang.domain.payment.domain.entity.PaymentAuditEventType
import com.moongchijang.domain.payment.domain.entity.PaymentAuditSource
import com.moongchijang.domain.payment.domain.entity.Payment
import com.moongchijang.domain.payment.domain.entity.PaymentOrder
import com.moongchijang.domain.payment.domain.entity.PaymentOrderStatus
import com.moongchijang.domain.payment.domain.repository.PaymentOrderRepository
import com.moongchijang.domain.payment.domain.repository.PaymentRepository
import com.moongchijang.domain.refund.application.RefundRequestSyncService
import com.moongchijang.domain.store.domain.repository.StoreStaffRepository
import com.moongchijang.domain.user.domain.repository.UserRepository
import com.moongchijang.global.config.PortOneProperties
import com.moongchijang.global.exception.CustomException
import com.moongchijang.global.exception.ErrorCode
import com.moongchijang.global.util.S3ImageReferenceResolver
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.UUID

@Service
class PaymentService(
    private val groupBuyRepository: GroupBuyRepository,
    private val userRepository: UserRepository,
    private val participationRepository: ParticipationRepository,
    private val favoriteRepository: FavoriteRepository,
    private val storeStaffRepository: StoreStaffRepository,
    private val paymentOrderRepository: PaymentOrderRepository,
    private val paymentRepository: PaymentRepository,
    private val paymentAuditLogService: PaymentAuditLogService,
    private val portOnePaymentPort: PortOnePaymentPort,
    private val portOneProperties: PortOneProperties,
    private val transactionManager: PlatformTransactionManager,
    private val redisLockUtil: RedisLockUtil,
    private val notificationEventPublisher: NotificationEventPublisher,
    private val adminDiscordAlertService: AdminDiscordAlertService,
    private val refundRequestSyncService: RefundRequestSyncService,
    private val s3ImageReferenceResolver: S3ImageReferenceResolver,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getCheckoutInfo(groupBuyId: Long, quantity: Int): CheckoutInfoResponse {
        validateQuantity(quantity)
        val groupBuy = groupBuyRepository.findWithStoreById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

        validateGroupBuyAvailable(groupBuy)
        validatePerUserLimit(groupBuy, quantity)
        validateRemainingQuantity(groupBuy, quantity)

        val amounts = calculateAmounts(groupBuy.price, quantity)
        return CheckoutInfoResponse(
            groupBuyId = groupBuy.id,
            storeName = groupBuy.store.name,
            productName = groupBuy.productName,
            thumbnailUrl = s3ImageReferenceResolver.resolveForRead(groupBuy.thumbnailKey),
            pickupDate = groupBuy.pickupDate,
            pickupTimeStart = groupBuy.pickupTimeStart,
            pickupTimeEnd = groupBuy.pickupTimeEnd,
            unitPrice = groupBuy.price,
            quantity = quantity,
            productAmount = amounts.productAmount,
            feeAmount = amounts.feeAmount,
            totalAmount = amounts.totalAmount,
            remainingQuantity = groupBuy.maxQuantity - groupBuy.currentQuantity,
        )
    }

    @Transactional
    fun createPaymentOrder(groupBuyId: Long, userId: Long, request: CreatePaymentOrderRequest): CreatePaymentOrderResponse {
        validateQuantity(request.quantity)
        validateAgreements(request)

        val user = userRepository.findByIdAndDeletedAtIsNull(userId)
            ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val groupBuy = groupBuyRepository.findWithLockById(groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }

        validateGroupBuyAvailable(groupBuy)
        validatePerUserLimit(groupBuy, request.quantity)
        validateRemainingQuantity(groupBuy, request.quantity)
        validateNotParticipated(userId, groupBuyId)

        val amounts = calculateAmounts(groupBuy.price, request.quantity)
        val order = paymentOrderRepository.save(
            PaymentOrder(
                orderId = generateOrderId(groupBuyId),
                user = user,
                groupBuy = groupBuy,
                quantity = request.quantity,
                productAmount = amounts.productAmount,
                feeAmount = amounts.feeAmount,
                totalAmount = amounts.totalAmount,
                agreedNoCancelAfterGoal = request.agreedNoCancelAfterGoal,
                agreedRefundBeforeGoal = request.agreedRefundBeforeGoal,
                agreedNoRefundAfterNoShow = request.agreedNoRefundAfterNoShow,
                agreedNoWithdrawal = request.agreedNoWithdrawal,
            )
        )

        return CreatePaymentOrderResponse(
            paymentId = order.orderId,
            storeId = portOneProperties.storeId,
            channelKey = portOneProperties.channelKey,
            orderName = "${groupBuy.productName} ${request.quantity}개",
            amount = order.totalAmount,
            customerName = user.nickname,
        )
    }

    fun completePortOnePayment(request: CompletePortOnePaymentRequest, userId: Long): ConfirmPaymentResponse {
        recordPaymentAudit(
            source = PaymentAuditSource.COMPLETE_API,
            eventType = PaymentAuditEventType.COMPLETE_REQUEST_RECEIVED,
            orderId = request.paymentId,
        )

        try {
            val order = transactionTemplate().execute {
                val foundOrder = paymentOrderRepository.findByOrderId(request.paymentId)
                    ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)
                validatePaymentOrderOwner(foundOrder, userId)
                foundOrder
            } ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)

            if (order.status == PaymentOrderStatus.APPROVED) {
                return transactionTemplate().execute {
                    val approvedOrder = paymentOrderRepository.findByOrderIdForUpdate(request.paymentId)
                        ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)
                    recordPaymentAudit(
                        source = PaymentAuditSource.COMPLETE_API,
                        eventType = PaymentAuditEventType.PAYMENT_IGNORED,
                        order = approvedOrder,
                        reason = ErrorCode.PAYMENT_ORDER_ALREADY_PROCESSED.name,
                    )
                    buildAlreadyApprovedResponse(approvedOrder)
                } ?: throw CustomException(ErrorCode.PAYMENT_ORDER_ALREADY_PROCESSED)
            }
            if (order.status != PaymentOrderStatus.READY) {
                throw CustomException(ErrorCode.PAYMENT_ORDER_ALREADY_PROCESSED)
            }
            if (order.totalAmount != request.amount) {
                recordPaymentFailure(
                    source = PaymentAuditSource.COMPLETE_API,
                    order = order,
                    reason = ErrorCode.PAYMENT_AMOUNT_MISMATCH.name,
                )
                throw CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
            }

            val paymentResult = getPortOnePaymentOrFailOrder(order.orderId)
            recordPaymentAudit(
                source = PaymentAuditSource.COMPLETE_API,
                eventType = PaymentAuditEventType.PORTONE_STATUS_FETCHED,
                order = order,
                paymentResult = paymentResult,
            )
            if (paymentResult.status != PORTONE_STATUS_PAID) {
                updateOrderFromPortOneStatus(order.orderId, paymentResult)
                recordPaymentFailure(
                    source = PaymentAuditSource.COMPLETE_API,
                    order = order,
                    paymentResult = paymentResult,
                    reason = ErrorCode.PAYMENT_APPROVAL_FAILED.name,
                )
                throw CustomException(ErrorCode.PAYMENT_APPROVAL_FAILED)
            }
            if (paymentResult.totalAmount != order.totalAmount || paymentResult.paymentId != order.orderId) {
                markOrderFailed(order.orderId)
                recordPaymentFailure(
                    source = PaymentAuditSource.COMPLETE_API,
                    order = order,
                    paymentResult = paymentResult,
                    reason = ErrorCode.PAYMENT_AMOUNT_MISMATCH.name,
                )
                throw CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
            }

            val result = withGroupBuyLock(order.groupBuy.id) {
                transactionTemplate().execute {
                    approvePayment(
                        paymentId = request.paymentId,
                        expectedAmount = request.amount,
                        paymentResult = paymentResult,
                        source = PaymentAuditSource.COMPLETE_API,
                    )
                } ?: PaymentApprovalResult.Failure(ErrorCode.PAYMENT_ORDER_ALREADY_PROCESSED)
            }

            return when (result) {
                is PaymentApprovalResult.Success -> result.response
                is PaymentApprovalResult.Failure -> throw CustomException(result.errorCode)
            }
        } catch (e: CustomException) {
            recordPaymentAudit(
                source = PaymentAuditSource.COMPLETE_API,
                eventType = PaymentAuditEventType.PAYMENT_FAILED,
                orderId = request.paymentId,
                reason = e.errorCode.name,
            )
            throw e
        }
    }

    private fun validatePaymentOrderOwner(order: PaymentOrder, userId: Long) {
        if (order.user.id != userId) {
            throw CustomException(ErrorCode.PAYMENT_ORDER_FORBIDDEN)
        }
    }

    fun handlePortOneWebhook(request: PortOneWebhookRequest, rawPayload: String? = null) {
        recordPaymentAudit(
            source = PaymentAuditSource.WEBHOOK,
            eventType = PaymentAuditEventType.WEBHOOK_RECEIVED,
            orderId = request.paymentId,
            rawPayload = rawPayload,
        )
        if (request.storeId != null && request.storeId != portOneProperties.storeId) {
            throw CustomException(ErrorCode.PAYMENT_WEBHOOK_INVALID)
        }

        val paymentId = request.paymentId
        if (paymentId.isNullOrBlank()) {
            throw CustomException(ErrorCode.PAYMENT_WEBHOOK_INVALID)
        }

        val order = transactionTemplate().execute {
            paymentOrderRepository.findByOrderId(paymentId)
        } ?: run {
            recordPaymentAudit(
                source = PaymentAuditSource.WEBHOOK,
                eventType = PaymentAuditEventType.PAYMENT_IGNORED,
                orderId = paymentId,
                reason = ErrorCode.PAYMENT_ORDER_NOT_FOUND.name,
                rawPayload = rawPayload,
            )
            return
        }
        val paymentResult = getPortOnePaymentOrFailOrder(order.orderId)
        recordPaymentAudit(
            source = PaymentAuditSource.WEBHOOK,
            eventType = PaymentAuditEventType.PORTONE_STATUS_FETCHED,
            order = order,
            paymentResult = paymentResult,
            rawPayload = rawPayload,
        )

        if (paymentResult.status == PORTONE_STATUS_PAID) {
            withGroupBuyLock(order.groupBuy.id) {
                transactionTemplate().execute {
                    val lockedOrder = paymentOrderRepository.findByOrderIdForUpdate(paymentId) ?: return@execute
                    if (lockedOrder.status != PaymentOrderStatus.APPROVED) {
                        approvePayment(
                            paymentId = paymentId,
                            expectedAmount = lockedOrder.totalAmount,
                            paymentResult = paymentResult,
                            source = PaymentAuditSource.WEBHOOK,
                        )
                    } else {
                        recordPaymentAudit(
                            source = PaymentAuditSource.WEBHOOK,
                            eventType = PaymentAuditEventType.PAYMENT_IGNORED,
                            order = lockedOrder,
                            paymentResult = paymentResult,
                            reason = ErrorCode.PAYMENT_ORDER_ALREADY_PROCESSED.name,
                            rawPayload = rawPayload,
                        )
                    }
                }
            }
            return
        }

        if (paymentResult.status == PORTONE_STATUS_CANCELLED || paymentResult.status == PORTONE_STATUS_PARTIAL_CANCELLED) {
            withGroupBuyLock(order.groupBuy.id) {
                transactionTemplate().execute {
                    val lockedOrder = paymentOrderRepository.findByOrderIdForUpdate(paymentId) ?: return@execute
                    when (paymentResult.status) {
                        PORTONE_STATUS_CANCELLED -> cancelPayment(lockedOrder, paymentResult, partial = false)
                        PORTONE_STATUS_PARTIAL_CANCELLED -> cancelPayment(lockedOrder, paymentResult, partial = true)
                    }
                }
            }
            return
        }

        transactionTemplate().execute {
            val lockedOrder = paymentOrderRepository.findByOrderIdForUpdate(paymentId) ?: return@execute
            if (paymentResult.status == PORTONE_STATUS_FAILED && lockedOrder.status == PaymentOrderStatus.READY) {
                val previousStatus = lockedOrder.status
                lockedOrder.fail(LocalDateTime.now())
                paymentOrderRepository.save(lockedOrder)
                recordPaymentAudit(
                    source = PaymentAuditSource.WEBHOOK,
                    eventType = PaymentAuditEventType.PAYMENT_FAILED,
                    order = lockedOrder,
                    paymentResult = paymentResult,
                    previousOrderStatus = previousStatus,
                    currentOrderStatus = lockedOrder.status,
                    rawPayload = rawPayload,
                    reason = PORTONE_STATUS_FAILED,
                    notifyFailure = true,
                )
            }
        }
    }

    fun cancelParticipation(
        participationId: Long,
        userId: Long,
        request: CancelParticipationRequest,
    ): CancelParticipationResponse {
        validateCancelReason(request)

        val groupBuyId = transactionTemplate().execute {
            val participation = participationRepository.findById(participationId)
                .orElseThrow { CustomException(ErrorCode.PARTICIPATION_NOT_FOUND) }
            validateParticipationOwner(participation, userId)
            participation.groupBuy.id
        } ?: throw CustomException(ErrorCode.PARTICIPATION_NOT_FOUND)

        return withGroupBuyLock(groupBuyId) {
            val target = transactionTemplate().execute {
                findCancellationTarget(participationId, userId)
            } ?: throw CustomException(ErrorCode.PARTICIPATION_NOT_FOUND)

            val paymentResult = portOnePaymentPort.cancelPayment(target.pgPaymentId, cancelReasonMessage(request))
            if (paymentResult.status != PORTONE_STATUS_CANCELLED) {
                throw CustomException(ErrorCode.PAYMENT_CANCEL_FAILED)
            }

            transactionTemplate().execute {
                applyParticipationCancellation(
                    target = target,
                    request = request,
                    cancelledAt = paymentResult.cancelledAt ?: LocalDateTime.now(),
                )
            } ?: throw CustomException(ErrorCode.PAYMENT_CANCEL_FAILED)
        }
    }

    fun processPendingRefunds(batchSize: Int = PENDING_REFUND_BATCH_SIZE): PendingRefundProcessingResult {
        val pageable = PageRequest.of(
            0,
            batchSize.coerceAtLeast(1),
            Sort.by(Sort.Order.asc("cancelledAt"), Sort.Order.asc("createdAt"), Sort.Order.asc("id"))
        )
        val participations = transactionTemplate().execute {
            participationRepository.findByStatusOrderByCancelledAtAscCreatedAtAsc(
                status = ParticipationStatus.REFUND_PENDING,
                pageable = pageable
            )
        } ?: emptyList()

        var successCount = 0
        var failedCount = 0
        participations.forEach { participation ->
            try {
                val success = processPendingRefund(participation.id)
                if (success) {
                    successCount++
                } else {
                    failedCount++
                }
            } catch (e: Exception) {
                log.error("[PaymentService] 환불대기 처리 중 예외 발생: participationId={}", participation.id, e)
                failedCount++
            }
        }

        return PendingRefundProcessingResult(
            targetCount = participations.size,
            successCount = successCount,
            failedCount = failedCount
        )
    }

    private fun approvePayment(
        paymentId: String,
        expectedAmount: Int,
        paymentResult: PortOnePaymentResult,
        source: PaymentAuditSource,
    ): PaymentApprovalResult {
        val order = paymentOrderRepository.findByOrderIdForUpdate(paymentId)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)

        if (order.status == PaymentOrderStatus.APPROVED) {
            return PaymentApprovalResult.Success(buildAlreadyApprovedResponse(order))
        }
        if (order.status != PaymentOrderStatus.READY) {
            throw CustomException(ErrorCode.PAYMENT_ORDER_ALREADY_PROCESSED)
        }
        if (order.totalAmount != expectedAmount) {
            return PaymentApprovalResult.Failure(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
        }
        if (paymentResult.totalAmount != order.totalAmount) {
            val previousStatus = order.status
            order.fail(LocalDateTime.now())
            paymentOrderRepository.save(order)
            recordPaymentFailure(
                source = source,
                order = order,
                paymentResult = paymentResult,
                previousOrderStatus = previousStatus,
                currentOrderStatus = order.status,
                reason = ErrorCode.PAYMENT_AMOUNT_MISMATCH.name,
            )
            return PaymentApprovalResult.Failure(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
        }
        if (paymentResult.paymentId != order.orderId) {
            val previousStatus = order.status
            order.fail(LocalDateTime.now())
            paymentOrderRepository.save(order)
            recordPaymentFailure(
                source = source,
                order = order,
                paymentResult = paymentResult,
                previousOrderStatus = previousStatus,
                currentOrderStatus = order.status,
                reason = ErrorCode.PAYMENT_AMOUNT_MISMATCH.name,
            )
            return PaymentApprovalResult.Failure(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
        }

        val userId = order.user.id ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        validateNotParticipated(userId, order.groupBuy.id)

        val updatedRows = groupBuyRepository.increaseCurrentQuantityIfAvailable(order.groupBuy.id, order.quantity)
        if (updatedRows == 0) {
            log.warn(
                "[PaymentService] 조건부 수량 증가 실패: groupBuyId={}, quantity={}, orderId={}",
                order.groupBuy.id, order.quantity, order.orderId
            )
            val previousStatus = order.status
            order.fail(LocalDateTime.now())
            paymentOrderRepository.save(order)
            recordPaymentFailure(
                source = source,
                order = order,
                paymentResult = paymentResult,
                previousOrderStatus = previousStatus,
                currentOrderStatus = order.status,
                reason = ErrorCode.PAYMENT_QUANTITY_EXCEEDED.name,
            )
            return PaymentApprovalResult.Failure(ErrorCode.PAYMENT_QUANTITY_EXCEEDED)
        }

        val groupBuy = groupBuyRepository.findById(order.groupBuy.id)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }
        val participationStatus = if (groupBuy.currentQuantity >= groupBuy.targetQuantity) {
            ParticipationStatus.CONFIRMED
        } else {
            ParticipationStatus.PAID_WAITING_GOAL
        }

        val participation = participationRepository.save(
            Participation(
                user = order.user,
                groupBuy = groupBuy,
                quantity = order.quantity,
                productAmount = order.productAmount,
                feeAmount = order.feeAmount,
                totalAmount = order.totalAmount,
                status = participationStatus,
            )
        )

        val achievedNow = groupBuy.currentQuantity >= groupBuy.targetQuantity && groupBuy.status == GroupBuyStatus.IN_PROGRESS
        if (achievedNow) {
            groupBuy.transitionToAchieved()
        }
        if (groupBuy.status == GroupBuyStatus.ACHIEVED && groupBuy.currentQuantity >= groupBuy.maxQuantity) {
            groupBuy.transitionToCompletedWhenMaxQuantityReached()
        }
        val approvedAt = paymentResult.paidAt ?: LocalDateTime.now()
        val previousStatus = order.status
        order.approve(approvedAt)
        val approvedOrder = paymentOrderRepository.save(order)

        paymentRepository.save(
            Payment(
                paymentOrder = approvedOrder,
                pgPaymentId = paymentResult.paymentId,
                orderId = order.orderId,
                amount = paymentResult.totalAmount,
                method = paymentResult.method,
                approvedAt = approvedAt,
            )
        )

        recordPaymentAudit(
            source = source,
            eventType = PaymentAuditEventType.PAYMENT_APPROVED,
            order = approvedOrder,
            paymentResult = paymentResult,
            previousOrderStatus = previousStatus,
            currentOrderStatus = approvedOrder.status,
        )

        publishApplyPaymentSuccessEvent(order, approvedAt)

        if (groupBuy.status == GroupBuyStatus.ACHIEVED) {
            val participantUserIds = publishApplyGroupBuyAchievedEvent(groupBuy.id, approvedAt)
            publishWishTargetAchievedEvent(groupBuy.id, approvedAt)
            publishRequestTargetAchievedEvent(groupBuy, approvedAt)
            if (achievedNow) {
                publishOwnerGroupBuyAchievedEvents(groupBuy.id, approvedAt)
                adminDiscordAlertService.sendGroupBuyAchieved(groupBuy, participantUserIds.size)
            }
        }

        publishRequestNewParticipantEvent(groupBuy, participation, approvedAt)

        return PaymentApprovalResult.Success(
            ConfirmPaymentResponse(
                paymentId = paymentResult.paymentId,
                participationId = participation.id,
                participationStatus = participation.status,
                displayStatus = displayStatus(participation.status),
                amount = paymentResult.totalAmount,
                method = paymentResult.method,
                approvedAt = approvedAt,
            )
        )
    }

    private fun publishApplyPaymentSuccessEvent(order: PaymentOrder, occurredAt: LocalDateTime) {
        val userId = order.user.id ?: return
        notificationEventPublisher.publishApplyPaymentSuccess(
            groupBuyId = order.groupBuy.id,
            orderId = order.orderId,
            userId = userId,
            occurredAt = occurredAt
        )
    }

    private fun publishApplyGroupBuyAchievedEvent(groupBuyId: Long, occurredAt: LocalDateTime): List<Long> {
        val participantUserIds = participationRepository.findDistinctUserIdsByGroupBuyId(groupBuyId)
        if (participantUserIds.isEmpty()) return emptyList()

        notificationEventPublisher.publishApplyGroupBuyAchieved(
            groupBuyId = groupBuyId,
            participantUserIds = participantUserIds,
            occurredAt = occurredAt
        )
        return participantUserIds
    }

    private fun publishWishTargetAchievedEvent(groupBuyId: Long, occurredAt: LocalDateTime) {
        val favoriteUserIds = favoriteRepository.findUserIdsByGroupBuyIdExcludingParticipants(groupBuyId)
        if (favoriteUserIds.isEmpty()) return

        notificationEventPublisher.publishWishTargetAchieved(
            groupBuyId = groupBuyId,
            userIds = favoriteUserIds,
            occurredAt = occurredAt
        )
    }

    private fun publishRequestNewParticipantEvent(
        groupBuy: GroupBuy,
        participation: Participation,
        occurredAt: LocalDateTime
    ) {
        val groupBuyRequest = groupBuy.groupBuyRequest ?: return
        val requesterUserId = groupBuyRequest.user.id ?: return
        val requestId = groupBuyRequest.id
        val participantUserId = participation.user.id ?: return
        if (requesterUserId == participantUserId) return

        notificationEventPublisher.publishRequestNewParticipant(
            requestId = requestId,
            requesterUserId = requesterUserId,
            participationId = participation.id,
            occurredAt = occurredAt
        )
    }

    private fun publishRequestTargetAchievedEvent(groupBuy: GroupBuy, occurredAt: LocalDateTime) {
        val groupBuyRequest = groupBuy.groupBuyRequest ?: return
        val requesterUserId = groupBuyRequest.user.id ?: return
        val requestId = groupBuyRequest.id
        notificationEventPublisher.publishRequestTargetAchieved(
            requestId = requestId,
            requesterUserId = requesterUserId,
            occurredAt = occurredAt
        )
    }

    private fun publishOwnerGroupBuyAchievedEvents(groupBuyId: Long, occurredAt: LocalDateTime) {
        val groupBuy = groupBuyRepository.findWithStoreById(groupBuyId).orElse(null) ?: return
        val ownerUserIds = storeStaffRepository.findUserIdsByStoreId(groupBuy.store.id).distinct()
        if (ownerUserIds.isEmpty()) return

        notificationEventPublisher.publishOwnerGroupBuyAchieved(
            groupBuyId = groupBuyId,
            ownerUserIds = ownerUserIds,
            occurredAt = occurredAt
        )
        notificationEventPublisher.publishOwnerOrderConfirmRequired(
            groupBuyId = groupBuyId,
            ownerUserIds = ownerUserIds,
            occurredAt = occurredAt
        )
    }

    private fun getPortOnePaymentOrFailOrder(paymentId: String): PortOnePaymentResult {
        return try {
            portOnePaymentPort.getPayment(paymentId)
        } catch (e: CustomException) {
            markOrderFailed(paymentId)
            throw e
        }
    }

    private fun markOrderFailed(paymentId: String) {
        requiresNewTransactionTemplate().execute {
            val order = paymentOrderRepository.findByOrderIdForUpdate(paymentId) ?: return@execute
            if (order.status == PaymentOrderStatus.READY) {
                val previousStatus = order.status
                order.fail(LocalDateTime.now())
                paymentOrderRepository.save(order)
                recordPaymentAudit(
                    source = PaymentAuditSource.COMPLETE_API,
                    eventType = PaymentAuditEventType.PAYMENT_FAILED,
                    order = order,
                    previousOrderStatus = previousStatus,
                    currentOrderStatus = order.status,
                    reason = ErrorCode.PAYMENT_APPROVAL_FAILED.name,
                    notifyFailure = true,
                )
            }
        }
    }

    private fun updateOrderFromPortOneStatus(paymentId: String, paymentResult: PortOnePaymentResult) {
        requiresNewTransactionTemplate().execute {
            val order = paymentOrderRepository.findByOrderIdForUpdate(paymentId) ?: return@execute
            val previousStatus = order.status
            when (paymentResult.status) {
                PORTONE_STATUS_CANCELLED -> order.cancel(paymentResult.cancelledAt ?: LocalDateTime.now())
                PORTONE_STATUS_PARTIAL_CANCELLED -> order.partialCancel(paymentResult.cancelledAt ?: LocalDateTime.now())
                else -> order.fail(LocalDateTime.now())
            }
            paymentOrderRepository.save(order)
            val eventType = when (paymentResult.status) {
                PORTONE_STATUS_CANCELLED -> PaymentAuditEventType.PAYMENT_CANCELLED
                PORTONE_STATUS_PARTIAL_CANCELLED -> PaymentAuditEventType.PAYMENT_PARTIAL_CANCELLED
                else -> PaymentAuditEventType.PAYMENT_FAILED
            }
            recordPaymentAudit(
                source = PaymentAuditSource.COMPLETE_API,
                eventType = eventType,
                order = order,
                paymentResult = paymentResult,
                previousOrderStatus = previousStatus,
                currentOrderStatus = order.status,
                reason = paymentResult.status,
                notifyFailure = eventType == PaymentAuditEventType.PAYMENT_FAILED,
            )
        }
    }

    private fun cancelPayment(order: PaymentOrder, paymentResult: PortOnePaymentResult, partial: Boolean) {
        val cancelledAt = paymentResult.cancelledAt ?: LocalDateTime.now()
        val payment = paymentRepository.findByPaymentOrderOrderId(order.orderId)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_ALREADY_PROCESSED)

        if (!partial) {
            applyParticipationRefundConsistency(order, cancelledAt)
        }

        updateOrderAndPaymentCancellationState(order, payment, paymentResult, cancelledAt, partial)
    }

    private fun updateOrderAndPaymentCancellationState(
        order: PaymentOrder,
        payment: Payment,
        paymentResult: PortOnePaymentResult,
        cancelledAt: LocalDateTime,
        partial: Boolean
    ) {
        val previousStatus = order.status
        if (partial) {
            order.partialCancel(cancelledAt)
            payment.partialCancel(cancelledAt)
            recordPaymentAudit(
                source = PaymentAuditSource.WEBHOOK,
                eventType = PaymentAuditEventType.PAYMENT_PARTIAL_CANCELLED,
                order = order,
                paymentResult = paymentResult,
                previousOrderStatus = previousStatus,
                currentOrderStatus = order.status,
            )
            return
        }

        order.cancel(cancelledAt)
        payment.cancel(cancelledAt)
        recordPaymentAudit(
            source = PaymentAuditSource.WEBHOOK,
            eventType = PaymentAuditEventType.PAYMENT_CANCELLED,
            order = order,
            paymentResult = paymentResult,
            previousOrderStatus = previousStatus,
            currentOrderStatus = order.status,
        )
    }

    private fun applyParticipationRefundConsistency(order: PaymentOrder, cancelledAt: LocalDateTime) {
        val userId = order.user.id ?: return
        val participation = participationRepository.findByUserIdAndGroupBuyId(userId, order.groupBuy.id) ?: return
        if (participation.status == ParticipationStatus.REFUNDED) {
            log.info(
                "[PaymentService] 환불 멱등 처리: orderId={}, userId={}, groupBuyId={}, participationId={}",
                order.orderId, userId, order.groupBuy.id, participation.id
            )
            return
        }

        // 취소 반영 시점의 최신 공구 상태를 락으로 재확인해서 수량 차감 정합성을 맞춘다.
        val groupBuy = groupBuyRepository.findWithLockById(order.groupBuy.id)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }
        validateRefundEligibility(groupBuy, order)
        val beforeQuantity = groupBuy.currentQuantity
        groupBuy.currentQuantity = (groupBuy.currentQuantity - participation.quantity).coerceAtLeast(0)
        participation.status = ParticipationStatus.REFUNDED
        participation.refundedAt = cancelledAt
        log.info(
            "[PaymentService] 환불 정합성 반영 완료: orderId={}, groupBuyId={}, participationId={}, quantity={}=>{}, refundedAt={}",
            order.orderId,
            groupBuy.id,
            participation.id,
            beforeQuantity,
            groupBuy.currentQuantity,
            cancelledAt
        )
    }

    private fun validateRefundEligibility(groupBuy: GroupBuy, order: PaymentOrder) {
        if (groupBuy.status == GroupBuyStatus.ACHIEVED || groupBuy.status == GroupBuyStatus.COMPLETED) {
            log.info(
                "[PaymentService] 환불 거절(달성 완료): orderId={}, groupBuyId={}, status={}",
                order.orderId, groupBuy.id, groupBuy.status
            )
            throw CustomException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED_AFTER_ACHIEVED)
        }
    }

    private fun findCancellationTarget(participationId: Long, userId: Long): CancellationTarget {
        val participation = participationRepository.findByIdForUpdate(participationId)
            .orElseThrow { CustomException(ErrorCode.PARTICIPATION_NOT_FOUND) }
        validateParticipationOwner(participation, userId)
        validateParticipationCancelable(participation)

        val order = paymentOrderRepository.findByUserIdAndGroupBuyId(userId, participation.groupBuy.id)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)
        validateApprovedPaymentOrder(order)
        val payment = paymentRepository.findByPaymentOrderOrderId(order.orderId)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)

        return CancellationTarget(
            participationId = participation.id,
            groupBuyId = participation.groupBuy.id,
            userId = userId,
            orderId = order.orderId,
            pgPaymentId = payment.pgPaymentId,
        )
    }

    private fun applyParticipationCancellation(
        target: CancellationTarget,
        request: CancelParticipationRequest,
        cancelledAt: LocalDateTime,
    ): CancelParticipationResponse {
        val participation = participationRepository.findByIdForUpdate(target.participationId)
            .orElseThrow { CustomException(ErrorCode.PARTICIPATION_NOT_FOUND) }
        validateParticipationOwner(participation, target.userId)
        validateParticipationCancelable(participation)

        val order = paymentOrderRepository.findByOrderIdForUpdate(target.orderId)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)
        validateApprovedPaymentOrder(order)
        val payment = paymentRepository.findByPaymentOrderOrderId(order.orderId)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)
        val groupBuy = groupBuyRepository.findWithLockById(target.groupBuyId)
            .orElseThrow { CustomException(ErrorCode.GROUPBUY_NOT_FOUND) }
        validateRefundEligibility(groupBuy, order)

        groupBuy.currentQuantity = (groupBuy.currentQuantity - participation.quantity).coerceAtLeast(0)
        order.cancel(cancelledAt)
        payment.cancel(cancelledAt)
        participation.status = ParticipationStatus.REFUNDED
        participation.cancelReason = request.reason
        participation.cancelReasonDetail = normalizedReasonDetail(request)
        participation.cancelledAt = cancelledAt
        participation.refundedAt = cancelledAt

        return CancelParticipationResponse(
            participationId = participation.id,
            status = participation.status,
            cancelledAt = cancelledAt,
            refundedAt = cancelledAt,
        )
    }

    private fun processPendingRefund(participationId: Long): Boolean {
        val target = transactionTemplate().execute {
            findPendingRefundTarget(participationId)
        } ?: return false

        val paymentResult = try {
            portOnePaymentPort.cancelPayment(
                paymentId = target.pgPaymentId,
                reason = PENDING_REFUND_CANCEL_REASON,
                cancelAmount = target.refundAmount,
            )
        } catch (e: CustomException) {
            log.warn(
                "[PaymentService] 환불대기 PG 취소 실패: participationId={}, orderId={}, errorCode={}",
                target.participationId,
                target.orderId,
                e.errorCode
            )
            return false
        }
        if (paymentResult.status != PORTONE_STATUS_CANCELLED && paymentResult.status != PORTONE_STATUS_PARTIAL_CANCELLED) {
            log.warn(
                "[PaymentService] 환불대기 PG 취소 미완료 상태: participationId={}, orderId={}, portOneStatus={}",
                target.participationId,
                target.orderId,
                paymentResult.status
            )
            return false
        }

        return transactionTemplate().execute {
            applyPendingRefundCompletion(
                target = target,
                refundedAt = paymentResult.cancelledAt ?: LocalDateTime.now()
            )
            true
        } ?: false
    }

    private fun findPendingRefundTarget(participationId: Long): PendingRefundTarget? {
        val participation = participationRepository.findByIdForUpdate(participationId)
            .orElseThrow { CustomException(ErrorCode.PARTICIPATION_NOT_FOUND) }
        if (participation.status != ParticipationStatus.REFUND_PENDING) {
            return null
        }
        // 사용자 환불 요청(cancelReason 존재)은 사장님 동의(APPROVED) 이후에만 환불 처리 대상으로 잡는다.
        if (participation.cancelReason != null && participation.ownerRefundReviewStatus != OwnerRefundReviewStatus.APPROVED) {
            return null
        }

        val userId = participation.user.id ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val order = paymentOrderRepository.findByUserIdAndGroupBuyIdForUpdate(userId, participation.groupBuy.id)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)
        validateApprovedPaymentOrder(order)
        val payment = paymentRepository.findByPaymentOrderOrderId(order.orderId)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)

        return PendingRefundTarget(
            participationId = participation.id,
            orderId = order.orderId,
            pgPaymentId = payment.pgPaymentId,
            refundAmount = participation.approvedRefundAmount
                ?: if (participation.cancelReason == null) {
                    // 자동 실패(목표 미달), 사장님 귀책 등 사용자 요청 사유가 없는 환불은 전액 환불
                    order.totalAmount
                } else {
                    (order.totalAmount - order.feeAmount.coerceAtLeast(0)).coerceAtLeast(0)
                },
        )
    }

    private fun applyPendingRefundCompletion(target: PendingRefundTarget, refundedAt: LocalDateTime) {
        val participation = participationRepository.findByIdForUpdate(target.participationId)
            .orElseThrow { CustomException(ErrorCode.PARTICIPATION_NOT_FOUND) }
        if (participation.status != ParticipationStatus.REFUND_PENDING) {
            return
        }

        val order = paymentOrderRepository.findByOrderIdForUpdate(target.orderId)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)
        val payment = paymentRepository.findByPaymentOrderOrderId(order.orderId)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_NOT_FOUND)

        val partial = target.refundAmount < order.totalAmount
        if (partial) {
            order.partialCancel(refundedAt)
            payment.partialCancel(refundedAt)
        } else {
            order.cancel(refundedAt)
            payment.cancel(refundedAt)
        }
        participation.status = ParticipationStatus.REFUNDED
        participation.refundedAt = refundedAt
        refundRequestSyncService.markCompleted(participation = participation, at = refundedAt)
    }

    private fun validateParticipationOwner(participation: Participation, userId: Long) {
        if (participation.user.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN)
        }
    }

    private fun validateParticipationCancelable(participation: Participation) {
        if (participation.status != ParticipationStatus.PAID_WAITING_GOAL) {
            throw CustomException(ErrorCode.PARTICIPATION_CANCEL_NOT_ALLOWED)
        }
    }

    private fun validateApprovedPaymentOrder(order: PaymentOrder) {
        if (order.status != PaymentOrderStatus.APPROVED) {
            throw CustomException(ErrorCode.PAYMENT_ORDER_ALREADY_PROCESSED)
        }
    }

    private fun buildAlreadyApprovedResponse(order: PaymentOrder): ConfirmPaymentResponse {
        val userId = order.user.id ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
        val participation = participationRepository.findByUserIdAndGroupBuyId(userId, order.groupBuy.id)
            ?: throw CustomException(ErrorCode.PAYMENT_ORDER_ALREADY_PROCESSED)
        val payment = paymentRepository.findByPaymentOrderOrderId(order.orderId)
        return ConfirmPaymentResponse(
            paymentId = payment?.pgPaymentId ?: order.orderId,
            participationId = participation.id,
            participationStatus = participation.status,
            displayStatus = displayStatus(participation.status),
            amount = order.totalAmount,
            method = payment?.method,
            approvedAt = order.approvedAt ?: LocalDateTime.now(),
        )
    }

    private fun validateQuantity(quantity: Int) {
        if (quantity < 1) throw CustomException(ErrorCode.PAYMENT_INVALID_QUANTITY)
    }

    private fun validateAgreements(request: CreatePaymentOrderRequest) {
        if (!request.agreedNoCancelAfterGoal ||
            !request.agreedRefundBeforeGoal ||
            !request.agreedNoRefundAfterNoShow ||
            !request.agreedNoWithdrawal
        ) {
            throw CustomException(ErrorCode.PAYMENT_AGREEMENT_REQUIRED)
        }
    }

    private fun validateGroupBuyAvailable(groupBuy: GroupBuy) {
        if (GroupBuyProgressCalculator.isClosed(groupBuy)) {
            throw CustomException(ErrorCode.PAYMENT_GROUPBUY_NOT_AVAILABLE)
        }
    }

    private fun validateRemainingQuantity(groupBuy: GroupBuy, quantity: Int) {
        if (groupBuy.currentQuantity + quantity > groupBuy.maxQuantity) {
            throw CustomException(ErrorCode.PAYMENT_QUANTITY_EXCEEDED)
        }
    }

    private fun validatePerUserLimit(groupBuy: GroupBuy, quantity: Int) {
        val perUserLimit = groupBuy.perUserLimit ?: return
        if (quantity > perUserLimit) {
            throw CustomException(ErrorCode.PAYMENT_PER_USER_LIMIT_EXCEEDED)
        }
    }

    private fun validateNotParticipated(userId: Long, groupBuyId: Long) {
        if (participationRepository.existsByUserIdAndGroupBuyId(userId, groupBuyId)) {
            throw CustomException(ErrorCode.PAYMENT_DUPLICATE_PARTICIPATION)
        }
    }

    private fun validateCancelReason(request: CancelParticipationRequest) {
        if (request.reason == ParticipationCancelReason.OTHER && request.reasonDetail.isNullOrBlank()) {
            throw CustomException(ErrorCode.PARTICIPATION_CANCEL_REASON_DETAIL_REQUIRED)
        }
    }

    private fun cancelReasonMessage(request: CancelParticipationRequest): String {
        val detail = normalizedReasonDetail(request)
        return listOfNotNull(request.reason.name, detail).joinToString(": ")
    }

    private fun normalizedReasonDetail(request: CancelParticipationRequest): String? =
        request.reasonDetail?.trim()?.takeIf { it.isNotBlank() }

    private fun calculateAmounts(unitPrice: Int, quantity: Int): PaymentAmounts {
        val productAmount = unitPrice * quantity
        val feeAmount = 0
        return PaymentAmounts(productAmount, feeAmount, productAmount + feeAmount)
    }

    private fun generateOrderId(groupBuyId: Long): String {
        return "MCJ-${groupBuyId}-${UUID.randomUUID().toString().replace("-", "").take(20)}"
    }

    private fun displayStatus(status: ParticipationStatus): String {
        return when (status) {
            ParticipationStatus.PAID_WAITING_GOAL -> "참여중 / 달성 전"
            ParticipationStatus.CONFIRMED -> "참여중 / 달성 완료"
            ParticipationStatus.CANCELLED -> "취소"
            ParticipationStatus.REFUND_PENDING -> "환불 대기"
            ParticipationStatus.REFUNDED -> "환불 완료"
            ParticipationStatus.PENDING -> "결제 대기"
        }
    }

    private fun transactionTemplate(): TransactionTemplate =
        TransactionTemplate(transactionManager)

    private fun <T> withGroupBuyLock(groupBuyId: Long, action: () -> T): T {
        val key = redisLockUtil.lockKey(groupBuyId)
        log.debug("[PaymentService] 공구 락 획득 시도: groupBuyId={}, key={}", groupBuyId, key)
        val token = redisLockUtil.tryLockOrThrow(key, waitMs = LOCK_WAIT_MS, leaseMs = LOCK_LEASE_MS)
        log.debug("[PaymentService] 공구 락 획득 성공: groupBuyId={}, key={}", groupBuyId, key)
        try {
            return action()
        } finally {
            val unlocked = redisLockUtil.unlock(key, token)
            if (!unlocked) {
                log.warn("[PaymentService] 공구 락 해제 실패: groupBuyId={}, key={}", groupBuyId, key)
            } else {
                log.debug("[PaymentService] 공구 락 해제 성공: groupBuyId={}, key={}", groupBuyId, key)
            }
        }
    }

    private fun requiresNewTransactionTemplate(): TransactionTemplate =
        TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

    private fun recordPaymentAudit(
        source: PaymentAuditSource,
        eventType: PaymentAuditEventType,
        order: PaymentOrder? = null,
        orderId: String? = order?.orderId,
        paymentResult: PortOnePaymentResult? = null,
        previousOrderStatus: PaymentOrderStatus? = null,
        currentOrderStatus: PaymentOrderStatus? = order?.status,
        reason: String? = null,
        rawPayload: String? = null,
        notifyFailure: Boolean = false,
    ) {
        paymentAuditLogService.record(
            PaymentAuditRecord(
                source = source,
                eventType = eventType,
                paymentOrder = order,
                orderId = orderId,
                pgPaymentId = paymentResult?.paymentId,
                previousOrderStatus = previousOrderStatus,
                currentOrderStatus = currentOrderStatus,
                pgStatus = paymentResult?.status,
                reason = reason,
                rawPayload = rawPayload,
                notifyFailure = notifyFailure,
            )
        )
    }

    private fun recordPaymentFailure(
        source: PaymentAuditSource,
        order: PaymentOrder,
        paymentResult: PortOnePaymentResult? = null,
        previousOrderStatus: PaymentOrderStatus? = null,
        currentOrderStatus: PaymentOrderStatus? = order.status,
        reason: String,
        rawPayload: String? = null,
    ) {
        recordPaymentAudit(
            source = source,
            eventType = PaymentAuditEventType.PAYMENT_FAILED,
            order = order,
            paymentResult = paymentResult,
            previousOrderStatus = previousOrderStatus,
            currentOrderStatus = currentOrderStatus,
            reason = reason,
            rawPayload = rawPayload,
            notifyFailure = true,
        )
    }

    private sealed interface PaymentApprovalResult {
        data class Success(val response: ConfirmPaymentResponse) : PaymentApprovalResult
        data class Failure(val errorCode: ErrorCode) : PaymentApprovalResult
    }

    private data class PaymentAmounts(
        val productAmount: Int,
        val feeAmount: Int,
        val totalAmount: Int,
    )

    private data class CancellationTarget(
        val participationId: Long,
        val groupBuyId: Long,
        val userId: Long,
        val orderId: String,
        val pgPaymentId: String,
    )

    private data class PendingRefundTarget(
        val participationId: Long,
        val orderId: String,
        val pgPaymentId: String,
        val refundAmount: Int,
    )

    companion object {
        private const val LOCK_WAIT_MS = 500L
        private const val LOCK_LEASE_MS = 10_000L
        private const val PENDING_REFUND_BATCH_SIZE = 100
        private const val PENDING_REFUND_CANCEL_REASON = "MINIMUM_QUANTITY_NOT_MET"
        private const val PORTONE_STATUS_PAID = "PAID"
        private const val PORTONE_STATUS_FAILED = "FAILED"
        private const val PORTONE_STATUS_CANCELLED = "CANCELLED"
        private const val PORTONE_STATUS_PARTIAL_CANCELLED = "PARTIAL_CANCELLED"
    }
}

data class PendingRefundProcessingResult(
    val targetCount: Int,
    val successCount: Int,
    val failedCount: Int,
)
