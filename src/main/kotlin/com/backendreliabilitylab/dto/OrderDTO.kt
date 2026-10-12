package com.backendreliabilitylab.dto

import java.util.UUID

data class CreateOrderRequestDTO(
    val productId: UUID,
    val quantity: Int
)

data class OrderResponseDTO(
    val uuid: UUID,
    val productId: UUID,
    val quantity: Int
)