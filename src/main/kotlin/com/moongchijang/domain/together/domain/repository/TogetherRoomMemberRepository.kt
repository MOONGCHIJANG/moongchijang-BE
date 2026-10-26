package com.moongchijang.domain.together.domain.repository

import com.moongchijang.domain.together.domain.entity.TogetherRoomMember
import org.springframework.data.jpa.repository.JpaRepository

interface TogetherRoomMemberRepository : JpaRepository<TogetherRoomMember, Long>
