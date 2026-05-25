package com.moongchijang.domain.user.domain.repository

import com.moongchijang.domain.user.domain.entity.UserRole
import com.moongchijang.domain.user.domain.entity.UserRoleAssignment
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleAssignmentRepository : JpaRepository<UserRoleAssignment, Long> {
    fun existsByUserIdAndRole(userId: Long, role: UserRole): Boolean

    fun findByUserId(userId: Long): List<UserRoleAssignment>
}
