package com.moongchijang.domain.user.domain.entity

import com.moongchijang.global.entity.BaseEntity
import com.moongchijang.global.time.TimePolicy
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.CascadeType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "users",
    indexes = [
        Index(name = "idx_users_provider_provider_id", columnList = "provider,provider_id"),
        Index(name = "uidx_users_provider_email", columnList = "provider,email", unique = true),
        Index(name = "uidx_users_provider_email_hash", columnList = "provider,email_hash", unique = true)
    ]
)
class User(

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var provider: AuthProvider,

    @Column(name = "provider_id", length = 100)
    var providerId: String? = null,

    @Column(length = 255)
    var email: String? = null,

    @Column(name = "email_hash", length = 64)
    var emailHash: String? = null,

    @Column(name = "password_hash", length = 255)
    var passwordHash: String? = null,

    @Column(length = 10)
    var nickname: String? = null,

    @Column(name = "phone_number", length = 255)
    var phoneNumber: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.BUYER,

    @Enumerated(EnumType.STRING)
    @Column(name = "last_role", length = 20)
    var lastRole: UserRole? = null,

    @Column(name = "signup_completed", nullable = false)
    var signupCompleted: Boolean = false,

    @Column(name = "seller_signup_completed", nullable = false)
    var sellerSignupCompleted: Boolean = false,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "withdrawal_reason", length = 50)
    var withdrawalReason: WithdrawalReason? = null,

    @Column(name = "withdrawal_reason_detail", length = 500)
    var withdrawalReasonDetail: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_withdrawal_reason", length = 50)
    var ownerWithdrawalReason: OwnerWithdrawalReason? = null,

    @Column(name = "owner_withdrawal_reason_detail", length = 500)
    var ownerWithdrawalReasonDetail: String? = null,

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    var roleAssignments: MutableSet<UserRoleAssignment> = mutableSetOf(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) : BaseEntity() {
    fun hasRole(role: UserRole): Boolean = roleAssignments.any { it.role == role }

    fun grantRole(role: UserRole) {
        if (hasRole(role)) {
            return
        }
        roleAssignments.add(UserRoleAssignment(user = this, role = role))
    }

    fun completeSignup(nickname: String, phoneNumber: String) {
        this.nickname = nickname
        this.phoneNumber = phoneNumber
        this.signupCompleted = true
    }

    fun completeSellerSignup() {
        this.sellerSignupCompleted = true
    }

    fun withdraw(
        reason: WithdrawalReason?,
        reasonDetail: String?,
        now: LocalDateTime = LocalDateTime.now(TimePolicy.STORAGE_ZONE_ID),
    ) {
        this.withdrawalReason = reason
        this.withdrawalReasonDetail = reasonDetail
        this.deletedAt = now
    }

    fun withdrawAsOwner(
        reason: OwnerWithdrawalReason?,
        reasonDetail: String?,
        now: LocalDateTime = LocalDateTime.now(TimePolicy.STORAGE_ZONE_ID),
    ) {
        this.ownerWithdrawalReason = reason
        this.ownerWithdrawalReasonDetail = reasonDetail
        this.deletedAt = now
    }

    fun anonymizePersonalInfoForWithdrawal() {
        this.providerId = null
        this.email = null
        this.passwordHash = null
        this.nickname = null
        this.phoneNumber = null
        this.signupCompleted = false
        this.sellerSignupCompleted = false
    }

    fun saveLastRole(role: UserRole) {
        this.lastRole = role
    }

    companion object {
        fun newKakaoUser(
            providerId: String,
            email: String?,
            emailHash: String?,
            nickname: String?,
        ): User {
            return User(
                provider = AuthProvider.KAKAO,
                providerId = providerId,
                email = email,
                emailHash = emailHash,
                nickname = nickname,
                role = UserRole.BUYER,
                signupCompleted = false,
            )
        }

        fun newEmailUser(
            email: String,
            emailHash: String,
            passwordHash: String,
        ): User {
            return User(
                provider = AuthProvider.EMAIL,
                providerId = null,
                email = email,
                emailHash = emailHash,
                passwordHash = passwordHash,
                role = UserRole.BUYER,
                signupCompleted = false,
            )
        }
    }
}
