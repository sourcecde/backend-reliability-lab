package com.backendreliabilitylab.repository

import com.backendreliabilitylab.entity.OutboxEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface OutboxEventRepository : JpaRepository<OutboxEvent, Long> {

    @Query(
        value = """
            SELECT *
            FROM outbox_events
            WHERE status = 'PENDING'
              AND next_attempt_at <= CURRENT_TIMESTAMP
            ORDER BY created_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
        """,
        nativeQuery = true
    )
    fun findPendingEventsForProcessing(
        @Param("batchSize") batchSize: Int
    ): List<OutboxEvent>
}