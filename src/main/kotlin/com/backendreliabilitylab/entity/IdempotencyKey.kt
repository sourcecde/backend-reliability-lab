package com.backendreliabilitylab.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "idempotency_keys")
class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "idempotency_key", nullable = false, unique = true)
    var idempotencyKey: UUID? = null

    @Column(name = "request_hash", nullable = false)
    var requestHash: String? = null

    @Column(name = "order_id")
    var orderId: Long? = null

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: LocalDateTime? = null
}