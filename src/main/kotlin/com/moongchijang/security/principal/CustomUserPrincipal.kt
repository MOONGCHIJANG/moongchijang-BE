package com.moongchijang.security.principal

import com.moongchijang.domain.user.entity.UserRole
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserPrincipal(
    val id: Long,
    val email: String?,
    val role: UserRole,
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${role.name}"))

    override fun getPassword(): String = ""

    override fun getUsername(): String = email ?: id.toString()
}

