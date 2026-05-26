package com.moongchijang.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = listOf(
        "http://localhost:3000",
        "http://localhost:5173",
    ),
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
    val allowedHeaders: List<String> = listOf("Authorization", "Content-Type", "Accept"),
    val allowCredentials: Boolean = true,
)
