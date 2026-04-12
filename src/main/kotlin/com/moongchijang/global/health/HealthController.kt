package com.moongchijang.global.health

import com.moongchijang.global.response.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Health", description = "서버 상태 확인을 위한 헬스체크 API")
@RestController
@RequestMapping("/health")
class HealthController {

    @Operation(summary = "헬스 체크", description = "정상적으로 서버가 실행 중인지 확인합니다.")
    @GetMapping
    fun check(): ResponseEntity<ApiResponse<Map<String, String>>> {
        return ResponseEntity.ok(
            ApiResponse.success(mapOf("status" to "UP"))
        )
    }

}
