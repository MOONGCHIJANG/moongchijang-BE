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
    name = "seller_business_profiles",
    indexes = [
        Index(name = "uidx_seller_business_profiles_user_id", columnList = "user_id", unique = true),
        Index(name = "uidx_seller_business_profiles_registration_no", columnList = "business_registration_number", unique = true),
    ],
)
class SellerBusinessProfile(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "business_registration_number", nullable = false, length = 20)
    var businessRegistrationNumber: String,

    @Column(name = "store_name", nullable = false, length = 100)
    var storeName: String,

    @Column(name = "owner_name", nullable = false, length = 50)
    var ownerName: String,

    @Column(name = "store_address", nullable = false, length = 255)
    var storeAddress: String,

    @Column(name = "phone_number", nullable = false, length = 20)
    var phoneNumber: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) : BaseEntity()
