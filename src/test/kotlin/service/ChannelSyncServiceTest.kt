package service

import org.ecommerce.service.ChannelSyncService

import org.ecommerce.event.InventoryUpdatedEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec
import reactor.core.publisher.Mono
import java.time.Duration

class ChannelSyncServiceTest {

    private lateinit var shopifyWebClient: WebClient
    private lateinit var channelSyncService: ChannelSyncService
    private lateinit var requestBodyUriSpec: RequestBodyUriSpec
    private lateinit var requestBodySpec: RequestBodySpec
    private lateinit var requestHeadersSpec: RequestHeadersSpec<*>
    private lateinit var responseSpec: ResponseSpec

    @BeforeEach
    fun setup() {
        shopifyWebClient = mock(WebClient::class.java)
        requestBodyUriSpec = mock(RequestBodyUriSpec::class.java)
        requestBodySpec = mock(RequestBodySpec::class.java)
        requestHeadersSpec = mock(RequestHeadersSpec::class.java)
        responseSpec = mock(ResponseSpec::class.java)

        `when`(shopifyWebClient.post()).thenReturn(requestBodyUriSpec)
        `when`(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec)
        `when`(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec)
        `when`(requestHeadersSpec.retrieve()).thenReturn(responseSpec)

        channelSyncService = ChannelSyncService(shopifyWebClient)
    }

    @Test
    fun `handleInventoryUpdated should successfully call Shopify API`() {
        // Mocking reactive Mono
        val mockResponseMono: Mono<Map<*, *>> = mock(Mono::class.java) as Mono<Map<*, *>>
        val expectedResponse = mapOf("inventory_level" to mapOf("available" to 50))
        `when`(responseSpec.bodyToMono(Map::class.java)).thenReturn(mockResponseMono)
        `when`(mockResponseMono.block(Duration.ofSeconds(5))).thenReturn(expectedResponse)

        val event = InventoryUpdatedEvent(
            orderId = "1001",
            sku = "TSHIRT-001",
            availableQuantity = 50,
            locationId = 12345L
        )

        // Call the listener
        channelSyncService.handleInventoryUpdated(event)

        // Verify interactions
        verify(shopifyWebClient).post()
        verify(requestBodyUriSpec).uri("/admin/api/latest/inventory_levels/set.json")
        verify(requestBodySpec).bodyValue(
            mapOf(
                "inventory_item_id" to event.orderId,
                "location_id" to event.locationId,
                "available" to event.availableQuantity
            )
        )
        verify(requestHeadersSpec).retrieve()
        verify(responseSpec).bodyToMono(Map::class.java)
        verify(mockResponseMono).block(Duration.ofSeconds(5))
    }

    @Test
    fun `handleInventoryUpdated should log error when Shopify call fails`() {
        val failingMono: Mono<Map<*, *>> = mock(Mono::class.java) as Mono<Map<*, *>>
        `when`(responseSpec.bodyToMono(Map::class.java)).thenReturn(failingMono)
        `when`(failingMono.block(Duration.ofSeconds(5))).thenThrow(RuntimeException("Shopify down"))

        val event = InventoryUpdatedEvent(
            orderId = "1002",
            sku = "TSHIRT-002",
            availableQuantity = 0,
            locationId = 12345L
        )

        // Call the listener
        channelSyncService.handleInventoryUpdated(event)

        // Should not throw, just logs error
        verify(shopifyWebClient).post()
    }
}
