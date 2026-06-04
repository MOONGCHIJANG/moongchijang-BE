package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.AuthProvider
import com.moongchijang.domain.user.domain.entity.User
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {

    // 활성(미탈퇴) 유저 조회
    fun findByProviderAndProviderIdAndDeletedAtIsNull(
        provider: AuthProvider,
        providerId: String,
    ): User?

    // 탈퇴 유저 조회
    fun findByProviderAndProviderIdAndDeletedAtIsNotNull(
        provider: AuthProvider,
        providerId: String,
    ): User?

    @Query(
        """
        select u
        from User u
        where u.provider = :provider
          and u.deletedAt is null
          and (u.emailHash = :emailHash or u.email = :legacyEmail)
        """
    )
    fun findActiveByProviderAndEmailHashOrLegacyEmail(
        @Param("provider") provider: AuthProvider,
        @Param("emailHash") emailHash: String,
        @Param("legacyEmail") legacyEmail: String,
    ): User?

    @Query(
        """
        select case when count(u) > 0 then true else false end
        from User u
        where u.provider = :provider
          and u.deletedAt is null
          and (u.emailHash = :emailHash or u.email = :legacyEmail)
        """
    )
    fun existsActiveByProviderAndEmailHashOrLegacyEmail(
        @Param("provider") provider: AuthProvider,
        @Param("emailHash") emailHash: String,
        @Param("legacyEmail") legacyEmail: String,
    ): Boolean

    fun existsByNicknameAndDeletedAtIsNull(nickname: String): Boolean

    fun existsByNicknameAndIdNotAndDeletedAtIsNull(nickname: String, id: Long): Boolean

    fun findByIdAndDeletedAtIsNull(id: Long): User?

    fun findByIdInAndDeletedAtIsNull(ids: Collection<Long>): List<User>

    @Query(
        """
        select u
        from User u
        where u.id > :lastId
          and (
            (u.email is not null and trim(u.email) <> '' and (u.emailHash is null or u.email not like 'enc:v1:%'))
            or (u.phoneNumber is not null and trim(u.phoneNumber) <> '' and u.phoneNumber not like 'enc:v1:%')
          )
        order by u.id asc
        """
    )
    fun findPersonalInfoBackfillTargets(
        @Param("lastId") lastId: Long,
        pageable: Pageable,
    ): List<User>
}
