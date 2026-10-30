package com.backendreliabilitylab.service

import com.backendreliabilitylab.repository.OutboxEventRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OutboxProcessor(
    private val outboxEventRepository: OutboxEventRepository,
    private val publisher: EventPublisher
) {

    @Scheduled(fixedDelay = 5000)
    @Transactional
    fun processPendingEvents() {

        val pendingEvents =
            outboxEventRepository.findPendingEventsForProcessing(10)

        pendingEvents.forEach { event ->
            try {
                publisher.publish(event)

                event.status = "PUBLISHED"
                event.publishedAt = LocalDateTime.now()

            } catch (exception: Exception) {
                event.retryCount += 1

                if (event.retryCount >= 5) {
                    event.status = "FAILED"
                } else {
                    val delaySeconds =
                        5L * (1L shl (event.retryCount - 1))

                    event.nextAttemptAt =
                        LocalDateTime.now().plusSeconds(delaySeconds)
                }
            }

            outboxEventRepository.save(event)
        }
    }
}