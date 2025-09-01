package org.ecommerce.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    @Bean
    fun shopifyWebClient(): WebClient =
        WebClient.builder()
            .baseUrl("https://your-shopify-api.com") // Replace with actual Shopify endpoint
            .defaultHeader("Content-Type", "application/json")
            .build()
}
