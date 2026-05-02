package com.moongchijang.domain.store.domain.entity

import com.moongchijang.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "stores")
class Store(

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 200)
    var address: String,

    @Column(name = "phone_number", length = 20)
    var phoneNumber: String? = null,

    @Column
    var latitude: Double? = null,

    @Column
    var longitude: Double? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false, length = 30)
    var region: RegionType,

    @Enumerated(EnumType.STRING)
    @Column(name = "district", nullable = false, length = 80)
    var district: DistrictType,


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L

) : BaseEntity()
