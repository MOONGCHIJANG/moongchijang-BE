package com.moongchijang.support.search

import com.moongchijang.domain.groupbuy.domain.entity.GroupBuyStatus
import org.mockito.ArgumentMatchers
import java.time.LocalDateTime

/**
 * Mockito 매처는 내부적으로 null을 반환하므로 Kotlin의 non-null 매개변수 위치에서 NPE가 발생한다.
 * 매처를 등록한 뒤 의미 없는 non-null sentinel 값을 반환해 컴파일/런타임 양쪽을 통과시킨다.
 */
object MockitoKotlinMatchers {
    fun anyLocalDateTime(): LocalDateTime {
        ArgumentMatchers.any(LocalDateTime::class.java)
        return LocalDateTime.MIN
    }

    fun anyGroupBuyStatus(): GroupBuyStatus {
        ArgumentMatchers.any(GroupBuyStatus::class.java)
        return GroupBuyStatus.IN_PROGRESS
    }

    fun anyLongList(): List<Long> {
        ArgumentMatchers.anyList<Long>()
        return emptyList()
    }
}
