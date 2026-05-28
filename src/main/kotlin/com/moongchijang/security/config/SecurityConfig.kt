package com.moongchijang.security.config

import com.moongchijang.global.config.CorsProperties
import com.moongchijang.security.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
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
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsProperties: CorsProperties,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/health",
                    "/api/v1/auth/kakao",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/email/**",
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
                    "/api/v1/group-buys/*/share",
                ).permitAll()
                it.requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/group-buys/*/viewers/heartbeat",
                ).permitAll()
                it.requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/pickups/*/verify",
                ).hasAnyRole("ADMIN", "SELLER")
                it.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

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

        configuration.allowedOrigins = corsProperties.allowedOrigins
        configuration.allowedMethods = corsProperties.allowedMethods
        configuration.allowedHeaders = corsProperties.allowedHeaders
        configuration.allowCredentials = corsProperties.allowCredentials

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
