package com.notahacker.otp_manager.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "otp_entries")
data class OtpEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false, unique = true)
    val username: String,
    @Column(nullable = false)
    val secret: String,
    @Column(nullable = false)
    val expiration: LocalDateTime
)