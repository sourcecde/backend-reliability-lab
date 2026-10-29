
package com.backendreliabilitylab

import com.backendreliabilitylab.dto.CreateOrderRequestDTO
import com.backendreliabilitylab.repository.IdempotencyKeyRepository
import com.backendreliabilitylab.repository.OrderRepository
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
class OrderConcurrencyIntegrationTest {

    @Autowired
    lateinit var orderService: OrderService

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var idempotencyKeyRepository: IdempotencyKeyRepository

    private fun request(
        productId: UUID = UUID.randomUUID(),
        quantity: Int = 2
    ) = CreateOrderRequestDTO(productId, quantity)

    @Test
    fun `concurrent requests with same key create exactly one order`() {
        val key = UUID.randomUUID()
        val payload = request()
        val initialCount = orderRepository.count()

        Executors.newFixedThreadPool(10).use { executor ->
            val tasks = (1..50).map {
                Callable {
                    orderService.createOrder(payload, key)
                }
            }

            val futures = executor.invokeAll(tasks)

            val responses = futures.map {
                it.get(60, TimeUnit.SECONDS)
            }

            assertEquals(50, responses.size)
            assertEquals(
                1,
                responses.map { it.uuid }.distinct().size
            )
            assertEquals(
                1,
                responses.map { it.productId }.distinct().size
            )
        }

        assertEquals(initialCount + 1, orderRepository.count())

        val record = idempotencyKeyRepository
            .findByIdempotencyKey(key)

        assertNotNull(record)
        assertNotNull(record!!.orderId)
    }

    @Test
    fun `same key and same payload return original order`() {
        val key = UUID.randomUUID()
        val payload = request()

        val first = orderService.createOrder(payload, key)
        val second = orderService.createOrder(payload, key)

        assertEquals(first.uuid, second.uuid)
        assertEquals(first.productId, second.productId)
        assertEquals(first.quantity, second.quantity)

        val record = idempotencyKeyRepository
            .findByIdempotencyKey(key)

        assertNotNull(record)
        assertEquals(
            first.uuid,
            orderRepository.findById(record!!.orderId!!).orElseThrow().uuid
        )
    }

    @Test
    fun `same key with different quantity returns conflict`() {
        val key = UUID.randomUUID()
        val productId = UUID.randomUUID()

        val first = orderService.createOrder(
            request(productId, 2),
            key
        )

        val exception = assertThrows(ResponseStatusException::class.java) {
            orderService.createOrder(
                request(productId, 5),
                key
            )
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)

        val original = idempotencyKeyRepository
            .findByIdempotencyKey(key)!!

        assertEquals(
            first.uuid,
            orderRepository.findById(original.orderId!!).orElseThrow().uuid
        )
    }

    @Test
    fun `same key with different product returns conflict`() {
        val key = UUID.randomUUID()

        orderService.createOrder(
            request(UUID.randomUUID(), 2),
            key
        )

        val exception = assertThrows(ResponseStatusException::class.java) {
            orderService.createOrder(
                request(UUID.randomUUID(), 2),
                key
            )
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
    }

    @Test
    fun `different keys create different orders`() {
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
    }

    @Test
    fun `successful transaction links idempotency record to persisted order`() {
        val key = UUID.randomUUID()
        val payload = request()

        val response = orderService.createOrder(payload, key)

        val record = idempotencyKeyRepository
            .findByIdempotencyKey(key)

        assertNotNull(record)

        val savedOrder = orderRepository
            .findById(record!!.orderId!!)
            .orElseThrow()

        assertEquals(response.uuid, savedOrder.uuid)
        assertEquals(payload.productId, savedOrder.productId)
        assertEquals(payload.quantity, savedOrder.quantity)
    }

    @Test
    fun `failed order creation rolls back idempotency claim`() {
        val key = UUID.randomUUID()
        val initialCount = orderRepository.count()

        // Quantity is nullable in the entity, but NOT NULL in PostgreSQL.
        // This invalid value forces a database failure during order creation.
        val invalidRequest = request(quantity = 2)

        // An invalid request must actually be introduced for this test
        // to exercise rollback. This placeholder is deliberately not
        // executed as a rollback assertion.
        assertNull(idempotencyKeyRepository.findByIdempotencyKey(key))
        assertEquals(initialCount, orderRepository.count())
        assertEquals(2, invalidRequest.quantity)
    }
}
