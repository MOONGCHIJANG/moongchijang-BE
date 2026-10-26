package com.moongchijang.domain.store.domain.entity

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
import java.time.LocalDate

@Entity
@Table(name = "store_holidays")
class StoreHoliday(

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    var store: Store,

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false, length = 20)
    var holidayType: StoreHolidayType,

    @Column(name = "day_of_week")
    var dayOfWeek: Int? = null,

    @Column(name = "holiday_date")
    var holidayDate: LocalDate? = null,

    @Column(length = 255)
    var reason: String? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
) : BaseEntity()
