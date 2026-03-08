package com.notahacker.otp_manager.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "otp_audit")
data class OtpAudit(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val username: String,
    @Column(nullable = false)
    val action: String,
    @Column(nullable = false)
    val timestamp: LocalDateTime
)