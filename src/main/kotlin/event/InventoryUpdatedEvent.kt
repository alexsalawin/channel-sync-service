package org.ecommerce.event

import java.time.OffsetDateTime

data class InventoryUpdatedEvent(
    val orderId: String,
    val sku: String,
    val availableQuantity: Int,
    val locationId: Long?,
    val timestamp: OffsetDateTime = OffsetDateTime.now()
)