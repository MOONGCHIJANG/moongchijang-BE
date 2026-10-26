package com.moongchijang.domain.together.domain.repository

import com.moongchijang.domain.together.domain.entity.PickupQrCode
import org.springframework.data.jpa.repository.JpaRepository

interface PickupQrCodeRepository : JpaRepository<PickupQrCode, Long>
