package com.notahacker.otp_manager.repository

import com.notahacker.otp_manager.model.OtpEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface OtpRepository : JpaRepository<OtpEntry, Long> {
    fun findByUsername(username: String): OtpEntry?
    @Transactional
    fun deleteByUsername(username: String)
}