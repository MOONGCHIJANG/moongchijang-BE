package com.moongchijang.domain.participation.domain.repository

import com.moongchijang.domain.participation.domain.entity.ParticipationConfirmation
import org.springframework.data.jpa.repository.JpaRepository

interface ParticipationConfirmationRepository : JpaRepository<ParticipationConfirmation, Long>
