package com.moongchijang.application.auth.dto.response

import com.moongchijang.domain.user.entity.AuthProvider
import com.moongchijang.domain.user.entity.User
import com.moongchijang.domain.user.entity.UserRole
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "인증 사용자 정보")
data class AuthUserResponse(

    @field:Schema(description = "사용자 ID", example = "1")
    val id: Long,

    @field:Schema(description = "로그인 제공자", example = "KAKAO")
    val provider: AuthProvider,

    @field:Schema(description = "소셜 제공자 사용자 ID", example = "1234567890")
    val providerId: String?,

    @field:Schema(description = "이메일", example = "user@example.com")
    val email: String?,

    @field:Schema(description = "닉네임", example = "문치장")
    val nickname: String?,

    @field:Schema(description = "전화번호", example = "010-1234-5678")
    val phoneNumber: String?,

    @field:Schema(description = "서비스 역할", example = "BUYER")
    val role: UserRole,

    @field:Schema(description = "추가정보 입력 완료 여부", example = "false")
    val signupCompleted: Boolean,

    @field:Schema(description = "삭제 시각(탈퇴 사용자일 때만 값 존재)", example = "2026-04-30T12:30:00")
    val deletedAt: LocalDateTime?,

    @field:Schema(description = "생성 시각", example = "2026-04-30T12:00:00")
    val createdAt: LocalDateTime,

    @field:Schema(description = "수정 시각", example = "2026-04-30T12:05:00")
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(user: User): AuthUserResponse = AuthUserResponse(
            id = user.id!!,
            provider = user.provider,
            providerId = user.providerId,
            email = user.email,
            nickname = user.nickname,
            phoneNumber = user.phoneNumber,
            role = user.role,
            signupCompleted = user.signupCompleted,
            deletedAt = user.deletedAt,
            createdAt = user.createdAt!!,
            updatedAt = user.updatedAt!!,
        )
    }
}
