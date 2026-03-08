package com.notahacker.otp_manager.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig(
    @Value("\${cors.allowed-origins:http://localhost:4321}") private val allowedOrigins: String
) {
    @Bean
    fun corsConfigurer(): WebMvcConfigurer = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            registry.addMapping("/otp/**")
                .allowedOrigins(*allowedOrigins.split(",").map { it.trim() }.toTypedArray())
                .allowedMethods("POST")
        }
    }
}
