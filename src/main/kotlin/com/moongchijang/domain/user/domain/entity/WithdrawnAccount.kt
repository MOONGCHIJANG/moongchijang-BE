package com.moongchijang.domain.user.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "withdrawn_accounts",
    uniqueConstraints = [
        UniqueConstraint(name = "uidx_withdrawn_accounts_provider_identifier_hash", columnNames = ["provider", "identifier_hash"]),
    ],
    indexes = [
        Index(name = "idx_withdrawn_accounts_rejoin_available_at", columnList = "rejoin_available_at"),
        Index(name = "idx_withdrawn_accounts_withdrawn_user_id", columnList = "withdrawn_user_id"),
    ],
)
class WithdrawnAccount(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var provider: AuthProvider,

    @Column(name = "identifier_hash", nullable = false, length = 64)
    var identifierHash: String,

    @Column(name = "withdrawn_user_id", nullable = false)
    var withdrawnUserId: Long,

    @Column(name = "withdrawn_at", nullable = false)
    var withdrawnAt: LocalDateTime,

    @Column(name = "rejoin_available_at", nullable = false)
    var rejoinAvailableAt: LocalDateTime,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) : BaseEntity()
