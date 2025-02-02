package com.nikichxp.warehouse.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.CorsRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
class CorsGlobalConfiguration : WebFluxConfigurer {

    override fun addCorsMappings(corsRegistry: CorsRegistry) {
        corsRegistry
            .addMapping("/**")
            .allowedOrigins("*")
            .allowedMethods("*")
            .maxAge(3600)
    }
}