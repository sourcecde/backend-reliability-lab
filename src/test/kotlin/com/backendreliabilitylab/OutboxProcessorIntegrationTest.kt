
package com.backendreliabilitylab

import com.backendreliabilitylab.entity.OutboxEvent
import com.backendreliabilitylab.repository.OutboxEventRepository
import com.backendreliabilitylab.service.EventPublisher
import com.backendreliabilitylab.service.OutboxProcessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Import(
    TestcontainersConfiguration::class,
    OutboxProcessorIntegrationTest.PublisherConfiguration::class
)
@SpringBootTest
@TestPropertySource(
    properties = [
        "spring.task.scheduling.enabled=false"
    ]
)
class OutboxProcessorIntegrationTest {

    @Autowired
    lateinit var processor: OutboxProcessor

    @Autowired
    lateinit var repository: OutboxEventRepository

    @Autowired
    lateinit var publisher: TestPublisher

    @TestConfiguration(proxyBeanMethods = false)
    class PublisherConfiguration {

        @Bean
        @Primary
        fun testPublisher(): TestPublisher = TestPublisher()
    }

    class TestPublisher : EventPublisher {

        val publishedIds = mutableListOf<UUID>()

        @Volatile
        var shouldFail = false

        @Volatile
        var publishStarted: CountDownLatch? = null

        @Volatile
        var releasePublisher: CountDownLatch? = null

        val publishCount = AtomicInteger(0)

        override fun publish(event: OutboxEvent) {
            publishCount.incrementAndGet()

            publishStarted?.countDown()

            releasePublisher?.let {
                check(it.await(10, TimeUnit.SECONDS)) {
                    "Timed out waiting to release publisher"
                }
            }

            if (shouldFail) {
                throw RuntimeException("Simulated broker failure")
            }

            synchronized(publishedIds) {
                publishedIds.add(event.eventId)
            }
        }

        fun reset() {
            shouldFail = false
            publishStarted = null
            releasePublisher = null
            publishCount.set(0)

            synchronized(publishedIds) {
                publishedIds.clear()
            }
        }
    }

    private fun createPendingEvent(
        retryCount: Int = 0
    ): OutboxEvent {
        val event = OutboxEvent().apply {
            aggregateType = "ORDER"
            aggregateId = UUID.randomUUID()
            eventType = "OrderCreated"
            payload = """{"quantity":2}"""
            status = "PENDING"
            this.retryCount = retryCount
            nextAttemptAt = LocalDateTime.now().minusMinutes(1)
        }

        return repository.saveAndFlush(event)
    }

    private fun prepareTest() {
        publisher.reset()

        // Ensure events left by earlier tests cannot be selected.
        repository.findAll()
            .filter { it.status == "PENDING" }
            .forEach {
                it.status = "FAILED"
                repository.save(it)
            }

        repository.flush()
    }

    @Test
    fun `successful publication marks event as published`() {
        prepareTest()
        val event = createPendingEvent()

        processor.processPendingEvents()

        val saved = repository.findById(event.id!!).orElseThrow()

        assertEquals("PUBLISHED", saved.status)
        assertNotNull(saved.publishedAt)
        assertEquals(1, publisher.publishCount.get())
        assertTrue(publisher.publishedIds.contains(event.eventId))
    }

    @Test
    fun `failed publication increments retry count and schedules retry`() {
        prepareTest()
        publisher.shouldFail = true

        val event = createPendingEvent()
        val beforeProcessing = LocalDateTime.now()

        processor.processPendingEvents()

        val saved = repository.findById(event.id!!).orElseThrow()

        assertEquals("PENDING", saved.status)
        assertEquals(1, saved.retryCount)
        assertTrue(saved.nextAttemptAt.isAfter(beforeProcessing))
        assertNull(saved.publishedAt)
        assertEquals(1, publisher.publishCount.get())
    }

    @Test
    fun `fifth failure moves event to failed status`() {
        prepareTest()
        publisher.shouldFail = true

        val event = createPendingEvent(retryCount = 4)

        processor.processPendingEvents()

        val saved = repository.findById(event.id!!).orElseThrow()

        assertEquals("FAILED", saved.status)
        assertEquals(5, saved.retryCount)
        assertNull(saved.publishedAt)
        assertEquals(1, publisher.publishCount.get())
    }

    @Test
    fun `concurrent processors do not publish same event twice`() {
        prepareTest()

        val event = createPendingEvent()

        val started = CountDownLatch(1)
        val release = CountDownLatch(1)

        publisher.publishStarted = started
        publisher.releasePublisher = release

        Executors.newFixedThreadPool(2).use { executor ->
            val first = executor.submit {
                processor.processPendingEvents()
            }

            try {
                assertTrue(
                    started.await(10, TimeUnit.SECONDS),
                    "First processor never started publishing"
                )

                val second = executor.submit {
                    processor.processPendingEvents()
                }

                second.get(10, TimeUnit.SECONDS)
            } finally {
                release.countDown()
            }

            first.get(10, TimeUnit.SECONDS)
        }

        val saved = repository.findById(event.id!!).orElseThrow()

        assertEquals(1, publisher.publishCount.get())
        assertEquals("PUBLISHED", saved.status)
    }
}
