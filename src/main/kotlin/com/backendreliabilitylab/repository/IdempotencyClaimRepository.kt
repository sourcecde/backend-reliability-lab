package com.backendreliabilitylab.repository

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class IdempotencyClaimRepository(
    private val jdbcTemplate: JdbcTemplate
) {
    fun claim(key: UUID, requestHash: String): Boolean {
        val insertedRows = jdbcTemplate.update(
            """
            INSERT INTO idempotency_keys (idempotency_key, request_hash)
            VALUES (?, ?)
            ON CONFLICT (idempotency_key) DO NOTHING
            """.trimIndent(),
            key,
            requestHash
        )

        return insertedRows == 1
    }
}