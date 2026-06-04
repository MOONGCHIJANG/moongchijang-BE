package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.WithdrawnParticipation
import org.springframework.data.jpa.repository.JpaRepository

interface WithdrawnParticipationRepository : JpaRepository<WithdrawnParticipation, Long>
