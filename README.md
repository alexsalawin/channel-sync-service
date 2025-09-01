# channel-sync-service

# High Level Diagram
<img width="806" height="601" alt="AnyX_HighLevel drawio" src="https://github.com/user-attachments/assets/5d7b29ea-2519-48ad-b423-3b268cd9bbe2" />

# Service Boundaries & Communication

Microservice decomposition:

### Order Service

**Responsibility**: Receives orders, validates them, orchestrates allocation and fulfillment.

**Boundaries**: Does not manage inventory directly but coordinates with inventory-service.

**Communication:**

**Synchronous:** Calls inventory-service to check stock (REST/gRPC depending on latency requirements).

**Asynchronous:** Publishes order events (Kafka/message queue) to trigger fulfillment and notifications.



### Inventory Service

**Responsibility:** Manages stock levels (local DB and WMS integration), prevents overselling.

**Boundaries:** Only manages stock, no order or fulfillment logic.

**Communication:**

**Synchronous:** REST/gRPC for stock queries from order-service.

**Asynchronous:** Listens to order allocation events to decrement stock or trigger WMS checks.



### Fulfillment Service

**Responsibility:** Sends orders to WMS for fulfillment.

**Boundaries:** Independent from order creation; only handles actual fulfillment.

**Communication:**

**Synchronous:** Call WMS API (REST) with retry/backoff.

**Asynchronous:** Listens to order-allocated events to initiate fulfillment.



### Channel Sync Service

**Responsibility:** Syncs inventory/stock changes to Shopify or other sales channels.

**Boundaries:** Independent from core order/fulfillment logic.

**Communication:**

**Asynchronous:** Listens to stock updates (Kafka) and publishes updates to Shopify (REST API).



**Justification:**

Each service has a single responsibility and owns its data.

Async events decouple services and improve resiliency.

Sync calls are used where immediate feedback is required (e.g., stock validation before order confirmation).

# Protocol choices:

**REST:** For external integration (Shopify, WMS) due to wide support.

**gRPC:** Optional internal high-performance calls between microservices if low latency is critical. (Order service, Inventory service)

**Message Queue (Kafka):** For async events like stock updates, order allocation, fulfillment triggers. Keep order service as orchestration and decoupled from database and external service layers.


# API Design and Data Mapping

**Shopify Webhook DTO:**

data class ShopifyOrderWebhookDTO(
    val id: Long,
    val name: String,
    val createdAt: ZonedDateTime,
    val currency: String,
    val financialStatus: String,
    val fulfillmentStatus: String?,
    val totalPrice: String,
    val locationId: Long,
    val lineItems: List<ShopifyLineItemDTO>
)

data class ShopifyLineItemDTO(
    val sku: String,
    val quantity: Int,
    val price: String
)


# Fields chosen:

**Included:** id, name, createdAt (order metadata), currency, financialStatus (payment validation), lineItems (to allocate stock), locationId (for WMS), totalPrice (optional validation).

**Omitted:** Customer personal info, shipping address (could be handled in fulfillment-service separately), Shopify-specific metadata not needed for stock/fulfillment.

# WMS Fulfillment API contract:

data class FulfillmentRequest(
    val orderId: String,
    val lineItems: List<FulfillmentLineItemDto>
)

data class FulfillmentLineItemDto(
    val sku: String,
    val quantity: Int
)

# Data Consistency & Race Conditions

Overselling prevention strategy:

Approach: Optimistic locking or atomic DB operations in inventory-service.

**Steps:**

Check available stock in DB.

Atomically decrement stock if sufficient quantity exists.

Publish allocation event for fulfillment.

**Trade-offs:**

Optimistic locking: Prevents overselling without locking entire table, but may require retries.

Pessimistic locking: Ensures correctness but reduces throughput in high-concurrency scenarios.

**Additional safeguards:**

WMS stock can be checked asynchronously, but local DB remains the source of truth for order allocation.

# Data Modeling

**PostgreSQL core tables:**

CREATE TABLE inventory (
    sku VARCHAR PRIMARY KEY,
    quantity INT NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    shopify_order_id BIGINT NOT NULL,
    name VARCHAR NOT NULL,
    total_price NUMERIC NOT NULL,
    currency VARCHAR NOT NULL,
    financial_status VARCHAR NOT NULL,
    fulfillment_status VARCHAR,
    location_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE order_line_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
    sku VARCHAR NOT NULL,
    quantity INT NOT NULL,
    price NUMERIC
);


**Design choices:**

inventory keyed by SKU for fast lookup and atomic updates.

Orders and line items are normalized for flexibility.

Foreign key ensures order-line integrity.

Timestamps for tracking and auditing.

# Error Handling & Resiliency

**Strategies:**

**WMS failures:**

Retry with exponential backoff (3 attempts, Thread.sleep backoff).

Log failures, push to dead-letter queue for manual intervention.

Channel Sync failures:

Publish stock updates asynchronously.

Retry failed events via message queue re-processing.

Shopify store update failures:

Retry via async event with idempotent updates (use SKU + order ID to prevent duplicate adjustments).

Alert system on repeated failures.

**Circuit Breakers:**

Prevent cascading failures if WMS or Shopify API is down.

