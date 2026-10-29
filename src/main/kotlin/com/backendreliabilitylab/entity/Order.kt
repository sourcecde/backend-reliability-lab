package com.backendreliabilitylab.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "orders")
class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    var uuid: UUID? = null
    var productId: UUID? = null
    var quantity: Int? = null
    @Column(insertable = false, updatable = false)
    var createdAt: LocalDateTime? = null
    @Column(insertable = false, updatable = false)
    var updatedAt: LocalDateTime? = null
}

