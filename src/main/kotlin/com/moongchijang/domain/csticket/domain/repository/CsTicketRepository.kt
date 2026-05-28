package com.moongchijang.domain.csticket.domain.repository

import com.moongchijang.domain.csticket.domain.entity.CsTicket
import com.moongchijang.domain.csticket.domain.entity.CsTicketStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface CsTicketRepository : JpaRepository<CsTicket, Long> {

    @Query(
        value = """
            select ticket
            from CsTicket ticket
            left join fetch ticket.consumer consumer
            left join fetch ticket.groupBuy groupBuy
            left join fetch groupBuy.store store
            where (:status is null or ticket.status = :status)
              and (
                :keyword is null
                or ticket.id = :ticketId
                or lower(coalesce(ticket.title, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(consumer.nickname, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(consumer.email, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(consumer.phoneNumber, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(groupBuy.productName, '')) like lower(concat('%', :keyword, '%'))
              )
            order by ticket.createdAt desc, ticket.id desc
        """,
        countQuery = """
            select count(ticket)
            from CsTicket ticket
            left join ticket.consumer consumer
            left join ticket.groupBuy groupBuy
            where (:status is null or ticket.status = :status)
              and (
                :keyword is null
                or ticket.id = :ticketId
                or lower(coalesce(ticket.title, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(consumer.nickname, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(consumer.email, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(consumer.phoneNumber, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(groupBuy.productName, '')) like lower(concat('%', :keyword, '%'))
              )
        """
    )
    fun findAdminPage(
        @Param("status") status: CsTicketStatus?,
        @Param("keyword") keyword: String?,
        @Param("ticketId") ticketId: Long?,
        pageable: Pageable,
    ): Page<CsTicket>

    @Query(
        """
            select ticket
            from CsTicket ticket
            left join fetch ticket.consumer
            left join fetch ticket.groupBuy groupBuy
            left join fetch groupBuy.store
            where ticket.id = :id
        """
    )
    fun findAdminDetailById(@Param("id") id: Long): Optional<CsTicket>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ticket from CsTicket ticket where ticket.id = :id")
    fun findWithLockById(@Param("id") id: Long): Optional<CsTicket>
}
