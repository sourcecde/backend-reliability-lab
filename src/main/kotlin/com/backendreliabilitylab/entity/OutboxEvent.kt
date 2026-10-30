package com.backendreliabilitylab.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Entity
@Table(name = "outbox_events")
class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "event_id", nullable = false, unique = true)
    var eventId: UUID = UUID.randomUUID()

    @Column(name = "aggregate_type", nullable = false)
    var aggregateType: String = ""

    @Column(name = "aggregate_id", nullable = false)
    lateinit var aggregateId: UUID

    @Column(name = "event_type", nullable = false)
    var eventType: String = ""

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    var payload: String = ""

    @Column(name = "status", nullable = false)
    var status: String = "PENDING"

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0

    @Column(name = "created_at", insertable = false, updatable = false)
    var createdAt: LocalDateTime? = null

    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null

    @Column(name = "next_attempt_at", nullable = false)
    var nextAttemptAt: LocalDateTime = LocalDateTime.now()
}