package com.backendreliabilitylab.service

import com.backendreliabilitylab.dto.CreateOrderRequestDTO
import com.backendreliabilitylab.dto.OrderResponseDTO
import com.backendreliabilitylab.entity.Order
import com.backendreliabilitylab.repository.IdempotencyClaimRepository
import com.backendreliabilitylab.repository.IdempotencyKeyRepository
import com.backendreliabilitylab.repository.OrderRepository
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import org.springframework.transaction.annotation.Transactional
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val idempotencyClaimRepository: IdempotencyClaimRepository,
    private val idempotencyKeyRepository: IdempotencyKeyRepository,
    ) {

    @Transactional
    fun createOrder(
        request: CreateOrderRequestDTO,
        idempotencyKey: UUID
    ): OrderResponseDTO {

        val requestHash = calculateRequestHash(request)

        val claimed = idempotencyClaimRepository.claim(
            idempotencyKey,
            requestHash
        )

        if (!claimed) {
            val existing = idempotencyKeyRepository
                .findByIdempotencyKey(idempotencyKey)
                ?: error("Idempotency record not found")

            if (existing.requestHash != requestHash) {
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Idempotency key already used with different request data"
                )
            }

            val existingOrder = orderRepository.findById(
                checkNotNull(existing.orderId)
            ).orElseThrow()

            return existingOrder.toResponse()
        }

        val order = Order().apply {
            uuid = UUID.randomUUID()
            productId = request.productId
            quantity = request.quantity
        }

        val savedOrder = orderRepository.saveAndFlush(order)

        val idempotencyRecord = idempotencyKeyRepository
            .findByIdempotencyKey(idempotencyKey)
            ?: error("Claimed idempotency record not found")

        idempotencyRecord.orderId = checkNotNull(savedOrder.id)

        idempotencyKeyRepository.save(idempotencyRecord)

        return savedOrder.toResponse()
    }

    fun updateOrder(orderId: Long, request: CreateOrderRequestDTO): OrderResponseDTO? {
        val existingOrder = orderRepository.findById(orderId).orElse(null)
        return if (existingOrder != null) {
            existingOrder.productId = request.productId
            existingOrder.quantity = request.quantity
            val updatedOrder = orderRepository.save(existingOrder)
            OrderResponseDTO(
                uuid = updatedOrder.uuid ?: UUID.randomUUID(),
                productId = updatedOrder.productId ?: UUID.randomUUID(),
                quantity = updatedOrder.quantity ?: 0
            )
        } else {
            null
        }
    }

    fun getAllOrders(): List<OrderResponseDTO> {
        return orderRepository.findAll().map { order ->
            OrderResponseDTO(
                uuid = order.uuid ?: UUID.randomUUID(),
                productId = order.productId?: UUID.randomUUID(),
                quantity = order.quantity ?: 0
            )
        }
    }

    fun getOrdersByProductId(productId: UUID): List<OrderResponseDTO> {
        return orderRepository.findByProductId(productId).map { order ->
            OrderResponseDTO(
                uuid = order.uuid ?: UUID.randomUUID(),
                productId = order.productId ?: UUID.randomUUID(),
                quantity = order.quantity ?: 0
            )
        }
    }


    fun getOrderById(orderId: Long): OrderResponseDTO? {
        val order = orderRepository.findById(orderId).orElse(null)
        return order?.let {
            OrderResponseDTO(
                uuid = it.uuid ?: UUID.randomUUID(),
                productId = it.productId ?: UUID.randomUUID(),
                quantity = it.quantity ?: 0
            )
        }
    }

    fun deleteOrder(orderId: Long): Boolean {
        return if (orderRepository.existsById(orderId)) {
            orderRepository.deleteById(orderId)
            true
        } else {
            false
        }
    }

    private fun calculateRequestHash(request: CreateOrderRequestDTO): String {
        val payload = "${request.productId}:${request.quantity}"

        val digest = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(StandardCharsets.UTF_8))

        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun Order.toResponse() = OrderResponseDTO(
        uuid = checkNotNull(uuid),
        productId = checkNotNull(productId),
        quantity = checkNotNull(quantity)
    )

}
