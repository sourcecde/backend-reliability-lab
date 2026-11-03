
package com.backendreliabilitylab

import com.backendreliabilitylab.dto.CreateOrderRequestDTO
import com.backendreliabilitylab.repository.OrderRepository
import com.backendreliabilitylab.service.OrderService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.StringRedisTemplate
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Import(TestcontainersConfiguration::class)
@SpringBootTest(properties = ["outbox.scheduling.enabled=false"])
class CacheStampedeIntegrationTest {

    @Autowired
    lateinit var orderService: OrderService

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var redisTemplate: StringRedisTemplate

    @Test
    fun `concurrent cache misses cause repeated database reads`() {
        val created = orderService.createOrder(
            CreateOrderRequestDTO(UUID.randomUUID(), 3),
            UUID.randomUUID()
        )

        val orderId = orderRepository.findAll()
            .first { it.uuid == created.uuid }
            .id!!

        redisTemplate.delete("order:$orderId")

        val before = orderService.databaseReadCount.get()

        val workers = 50
        val ready = CountDownLatch(workers)
        val start = CountDownLatch(1)

        val executor = Executors.newFixedThreadPool(workers)

        try {
            val futures = (1..workers).map {
                executor.submit(Callable {
                    ready.countDown()
                    check(start.await(10, TimeUnit.SECONDS))
                    orderService.getOrderById(orderId)
                })
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS))
            start.countDown()

            val results = futures.map {
                it.get(30, TimeUnit.SECONDS)
            }

            assertEquals(workers, results.size)
            assertTrue(results.all { it?.uuid == created.uuid })

            val databaseReads =
                orderService.databaseReadCount.get() - before

            println("Concurrent requests: $workers")
            println("Database lookup attempts: $databaseReads")

            assertTrue(databaseReads >= 1)
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }
}
