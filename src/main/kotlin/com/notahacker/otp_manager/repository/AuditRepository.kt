package com.notahacker.otp_manager.repository

import com.notahacker.otp_manager.model.OtpAudit
import org.springframework.data.jpa.repository.JpaRepository

interface AuditRepository : JpaRepository<OtpAudit, Long>