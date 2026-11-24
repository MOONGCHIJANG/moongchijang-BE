package com.moongchijang.domain.participation.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "participation_confirmations")
class ParticipationConfirmation(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    var participation: Participation,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ParticipationConfirmationHistoryStatus = ParticipationConfirmationHistoryStatus.REQUESTED,

    @Column(name = "requested_at", nullable = false)
    var requestedAt: LocalDateTime,

    @Column(name = "responded_at")
    var respondedAt: LocalDateTime? = null,

    @Column(nullable = false)
    var deadline: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity()

enum class ParticipationConfirmationHistoryStatus {
    REQUESTED,
    CONFIRMED,
    AUTO_EXCLUDED,
}
