package com.backendreliabilitylab.repository

import com.backendreliabilitylab.entity.IdempotencyKey
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdempotencyKeyRepository : JpaRepository<IdempotencyKey, Long> {

    fun findByIdempotencyKey(idempotencyKey: UUID): IdempotencyKey?
}