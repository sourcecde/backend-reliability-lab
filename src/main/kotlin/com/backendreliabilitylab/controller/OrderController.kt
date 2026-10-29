package com.backendreliabilitylab.controller

import com.backendreliabilitylab.dto.CreateOrderRequestDTO
import com.backendreliabilitylab.dto.OrderResponseDTO
import com.backendreliabilitylab.service.OrderService
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import org.springframework.web.bind.annotation.RequestHeader

@RestController
@RequestMapping("/api/order")
class OrderController (private val orderService: OrderService) {

    @GetMapping
    fun getAllOrders(): List<OrderResponseDTO> {
        return orderService.getAllOrders()
    }

    @GetMapping("/product/{productId}")
    fun getOrdersByProductId(productId: UUID): List<OrderResponseDTO> {
        return orderService.getOrdersByProductId(productId)
    }

    @GetMapping("/{orderId}")
    fun getOrderById(orderId: Long): OrderResponseDTO? {
        return orderService.getOrderById(orderId)
    }

    @PostMapping("/create")
    fun createOrder(
        @RequestHeader("Idempotency-Key") idempotencyKey: UUID,
        @RequestBody request: CreateOrderRequestDTO
    ): OrderResponseDTO {
        return orderService.createOrder(request, idempotencyKey)
    }

    @PutMapping("/update/{orderId}")
    fun updateOrder(orderId: Long, @RequestBody request: CreateOrderRequestDTO): OrderResponseDTO? {
        return orderService.updateOrder(orderId, request)
    }

    @DeleteMapping("/delete/{orderId}")
    fun deleteOrder(orderId: Long): Boolean {
        return orderService.deleteOrder(orderId)
    }

    
}