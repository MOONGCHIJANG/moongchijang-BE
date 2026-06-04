package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.WithdrawnRefundRequest
import org.springframework.data.jpa.repository.JpaRepository

interface WithdrawnRefundRequestRepository : JpaRepository<WithdrawnRefundRequest, Long>
