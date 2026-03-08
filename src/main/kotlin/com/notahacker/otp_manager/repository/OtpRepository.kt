package com.notahacker.otp_manager.repository

import com.notahacker.otp_manager.model.OtpEntry
import org.springframework.data.jpa.repository.JpaRepository

interface OtpRepository : JpaRepository<OtpEntry, Long> {
    fun findByUsername(username: String): OtpEntry?
}