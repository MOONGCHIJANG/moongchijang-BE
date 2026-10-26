package com.moongchijang.domain.together.domain.repository

import com.moongchijang.domain.together.domain.entity.TogetherRoom
import org.springframework.data.jpa.repository.JpaRepository

interface TogetherRoomRepository : JpaRepository<TogetherRoom, Long>
