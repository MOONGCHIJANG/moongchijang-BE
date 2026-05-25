package com.moongchijang.domain.user.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "seller_settlement_accounts",
    indexes = [
        Index(name = "uidx_seller_settlement_accounts_user_id", columnList = "user_id", unique = true),
    ],
)
class SellerSettlementAccount(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "bank_code", nullable = false, length = 80)
    var bankCode: String,

    @Column(name = "account_number", nullable = false, length = 50)
    var accountNumber: String,

    @Column(name = "account_holder_name", nullable = false, length = 50)
    var accountHolderName: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) : BaseEntity()
