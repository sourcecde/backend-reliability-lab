package com.backendreliabilitylab.service

import com.backendreliabilitylab.dto.OrderResponseDTO
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import tools.jackson.databind.ObjectMapper

@Service
class OrderCacheService(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper
) {
    private val ttl = Duration.ofMinutes(5)

    fun get(orderId: Long): OrderResponseDTO? {
        val json = redisTemplate.opsForValue()
            .get("order:$orderId") ?: return null

        return objectMapper.readValue(
            json,
            OrderResponseDTO::class.java
        )
    }

    fun put(orderId: Long, order: OrderResponseDTO) {
        val json = objectMapper.writeValueAsString(order)

        redisTemplate.opsForValue().set(
            "order:$orderId",
            json,
            ttl
        )
    }

    fun evict(orderId: Long) {
        redisTemplate.delete("order:$orderId")
    }
}