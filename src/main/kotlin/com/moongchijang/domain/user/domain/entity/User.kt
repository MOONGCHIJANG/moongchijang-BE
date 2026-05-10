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
import java.time.LocalDateTime

@Entity
@Table(name = "users",
    indexes = [
        Index(name = "idx_users_provider_provider_id", columnList = "provider,provider_id"),
        Index(name = "idx_users_email", columnList = "email")
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

    @Column(name = "password_hash", length = 255)
    var passwordHash: String? = null,

    @Column(length = 10)
    var nickname: String? = null,

    @Column(name = "phone_number", length = 20)
    var phoneNumber: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.BUYER,

    @Column(name = "signup_completed", nullable = false)
    var signupCompleted: Boolean = false,

    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
) : BaseEntity() {
    fun completeSignup(nickname: String, phoneNumber: String) {
        this.nickname = nickname
        this.phoneNumber = phoneNumber
        this.signupCompleted = true
    }

    fun withdraw(now: LocalDateTime = LocalDateTime.now()) {
        this.deletedAt = now
    }

    companion object {
        fun newKakaoUser(
            providerId: String,
            email: String?,
            nickname: String?,
        ): User {
            return User(
                provider = AuthProvider.KAKAO,
                providerId = providerId,
                email = email,
                nickname = nickname,
                role = UserRole.BUYER,
                signupCompleted = false,
            )
        }

        fun newEmailUser(
            email: String,
            passwordHash: String,
        ): User {
            return User(
                provider = AuthProvider.EMAIL,
                providerId = null,
                email = email,
                passwordHash = passwordHash,
                role = UserRole.BUYER,
                signupCompleted = false,
            )
        }
    }
}
