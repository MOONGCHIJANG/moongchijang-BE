package com.moongchijang.domain.payment.domain.repository

import com.moongchijang.domain.payment.domain.entity.PaymentAuditLog
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentAuditLogRepository : JpaRepository<PaymentAuditLog, Long>
