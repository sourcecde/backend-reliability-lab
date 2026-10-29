package com.backendreliabilitylab.repository

import com.backendreliabilitylab.entity.Order
import org.springframework.data.jpa.repository.JpaRepository

    interface OrderRepository : JpaRepository<Order, Long> {

        fun findByProductId(productId: java.util.UUID): List<Order>

    }
