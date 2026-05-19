package com.moongchijang.security.config

import com.moongchijang.security.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.http.HttpMethod
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors {  }
        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/health",
                    "/api/v1/auth/kakao",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/email/availability",
                    "/api/v1/auth/email/signup",
                    "/api/v1/auth/email/login",
                    "/api/v1/auth/email/verification-codes",
                    "/api/v1/auth/email/verification-codes/verify",
                    "/api/v1/auth/phone/verification-codes",
                    "/api/v1/auth/phone/verification-codes/verify",
                    "/api/v1/payments/portone/webhook",
                    "/openapi.yaml",
                    "/dev/openapi.yaml",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                ).permitAll()
                it.requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/group-buys",
                    "/api/v1/group-buys/*",
                    "/api/v1/group-buys/progress",
                    "/api/v1/group-buys/*/progress",
                ).permitAll()

                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .formLogin { it.disable() }
            .httpBasic { it.disable() }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        configuration.allowedOrigins = listOf(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://43.203.191.30",
            "https://www.moongchijang.com",
            "https://api.moongchijang.com"
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
