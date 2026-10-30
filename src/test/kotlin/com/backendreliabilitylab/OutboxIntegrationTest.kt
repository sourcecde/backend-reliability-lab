
package com.backendreliabilitylab

import com.backendreliabilitylab.dto.CreateOrderRequestDTO
import com.backendreliabilitylab.repository.IdempotencyKeyRepository
import com.backendreliabilitylab.repository.OrderRepository
import com.backendreliabilitylab.repository.OutboxEventRepository
import com.backendreliabilitylab.service.OrderService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Import(TestcontainersConfiguration::class)
@SpringBootTest
class OutboxIntegrationTest {

    @Autowired
    lateinit var orderService: OrderService

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var outboxEventRepository: OutboxEventRepository

    @Autowired
    lateinit var idempotencyKeyRepository: IdempotencyKeyRepository

    private fun request(
        productId: UUID = UUID.randomUUID(),
        quantity: Int = 2
    ) = CreateOrderRequestDTO(productId, quantity)

    private fun eventsFor(orderId: UUID) =
        outboxEventRepository.findAll()
            .filter { it.aggregateId == orderId }

    @Test
    fun `creating order also creates one outbox event`() {
        val key = UUID.randomUUID()
        val payload = request()

        val response = orderService.createOrder(payload, key)
        val events = eventsFor(response.uuid)

        assertEquals(1, events.size)

        val event = events.single()

        assertNotNull(event.eventId)
        assertEquals("ORDER", event.aggregateType)
        assertEquals(response.uuid, event.aggregateId)
        assertEquals("OrderCreated", event.eventType)
        assertEquals("PENDING", event.status)
        assertEquals(0, event.retryCount)
        assertNull(event.publishedAt)
    }

    @Test
    fun `outbox payload contains correct order details`() {
        val payload = request(quantity = 5)
        val response = orderService.createOrder(
            payload,
            UUID.randomUUID()
        )

        val event = eventsFor(response.uuid).single()

        assertTrue(event.payload.contains(response.uuid.toString()))
        assertTrue(event.payload.contains(payload.productId.toString()))
        assertTrue(event.payload.contains("\"quantity\": 5"))
    }

    @Test
    fun `same idempotency key does not create duplicate event`() {
        val key = UUID.randomUUID()
        val payload = request()

        val first = orderService.createOrder(payload, key)
        val second = orderService.createOrder(payload, key)

        assertEquals(first.uuid, second.uuid)
        assertEquals(1, eventsFor(first.uuid).size)
    }

    @Test
    fun `different keys create different orders and events`() {
        val payload = request()

        val first = orderService.createOrder(
            payload,
            UUID.randomUUID()
        )

        val second = orderService.createOrder(
            payload,
            UUID.randomUUID()
        )

        assertNotEquals(first.uuid, second.uuid)
        assertEquals(1, eventsFor(first.uuid).size)
        assertEquals(1, eventsFor(second.uuid).size)

        assertNotEquals(
            eventsFor(first.uuid).single().eventId,
            eventsFor(second.uuid).single().eventId
        )
    }

    @Test
    fun `same key with different payload creates no additional event`() {
        val key = UUID.randomUUID()
        val productId = UUID.randomUUID()

        val first = orderService.createOrder(
            request(productId, 2),
            key
        )

        val initialEventCount = outboxEventRepository.count()

        val exception = assertThrows(ResponseStatusException::class.java) {
            orderService.createOrder(
                request(productId, 5),
                key
            )
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
        assertEquals(initialEventCount, outboxEventRepository.count())
        assertEquals(1, eventsFor(first.uuid).size)
    }

    @Test
    fun `50 concurrent retries create one order and one event`() {
        val key = UUID.randomUUID()
        val payload = request(quantity = 3)

        val initialOrderCount = orderRepository.count()
        val initialEventCount = outboxEventRepository.count()

        val responses = Executors.newFixedThreadPool(10).use { executor ->
            val tasks = (1..50).map {
                Callable {
                    orderService.createOrder(payload, key)
                }
            }

            executor.invokeAll(tasks).map {
                it.get(60, TimeUnit.SECONDS)
            }
        }

        val uniqueOrderIds = responses.map { it.uuid }.distinct()

        assertEquals(50, responses.size)
        assertEquals(1, uniqueOrderIds.size)
        assertEquals(initialOrderCount + 1, orderRepository.count())
        assertEquals(initialEventCount + 1, outboxEventRepository.count())

        val events = eventsFor(uniqueOrderIds.single())

        assertEquals(1, events.size)
        assertEquals("PENDING", events.single().status)
    }

    @Test
    fun `outbox event references persisted order`() {
        val key = UUID.randomUUID()
        val response = orderService.createOrder(request(), key)

        val idempotencyRecord = idempotencyKeyRepository
            .findByIdempotencyKey(key)

        assertNotNull(idempotencyRecord)

        val savedOrder = orderRepository
            .findById(idempotencyRecord!!.orderId!!)
            .orElseThrow()

        val event = eventsFor(savedOrder.uuid!!).single()

        assertEquals(savedOrder.uuid, event.aggregateId)
        assertEquals(response.uuid, event.aggregateId)
    }
}
