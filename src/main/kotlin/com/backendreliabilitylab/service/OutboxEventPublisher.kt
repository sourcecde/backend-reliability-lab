package com.backendreliabilitylab.service

import com.backendreliabilitylab.entity.OutboxEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface EventPublisher {
    fun publish(event: OutboxEvent)
}

@Component
class OutboxEventPublisher : EventPublisher {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun publish(event: OutboxEvent) {
        logger.info(
            "Publishing event: eventId={}, type={}, payload={}",
            event.eventId,
            event.eventType,
            event.payload
        )
    }
}