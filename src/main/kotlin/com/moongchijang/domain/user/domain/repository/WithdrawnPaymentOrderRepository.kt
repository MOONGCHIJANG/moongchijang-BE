package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.WithdrawnPaymentOrder
import org.springframework.data.jpa.repository.JpaRepository

interface WithdrawnPaymentOrderRepository : JpaRepository<WithdrawnPaymentOrder, Long>
