package org.ecommerce.service

import org.ecommerce.event.InventoryUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Service
class ChannelSyncService(
    private val shopifyWebClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(ChannelSyncService::class.java)

    @KafkaListener(
        topics = ["inventory-updates"],
        groupId = "channel-sync-service"
    )
    fun handleInventoryUpdated(event: InventoryUpdatedEvent) {
        logger.info("Received inventory update event for SKU ${event.sku}, available: ${event.availableQuantity}")

        try {
            val response = shopifyWebClient.post()
                .uri("/admin/api/latest/inventory_levels/set.json")
                .bodyValue(
                    mapOf(
                        "inventory_item_id" to event.orderId,
                        "location_id" to event.locationId,
                        "available" to event.availableQuantity
                    )
                )
                .retrieve()
                .bodyToMono(Map::class.java)
                .block(Duration.ofSeconds(5))

            logger.info("Shopify stock update response for SKU ${event.sku}: $response")
        } catch (ex: Exception) {
            logger.error("Failed to sync SKU ${event.sku} to Shopify: ${ex.message}", ex)
            // Optional: retry mechanism or dead-letter topic for failed syncs
        }
    }
}
