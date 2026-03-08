package com.notahacker.otp_manager.service

import com.notahacker.otp_manager.model.OtpAudit
import com.notahacker.otp_manager.model.OtpEntry
import com.notahacker.otp_manager.repository.AuditRepository
import com.notahacker.otp_manager.repository.OtpRepository
import com.warrenstrange.googleauth.GoogleAuthenticator
import net.glxn.qrgen.javase.QRCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.Base64

@Service
class OtpService(
    private val otpRepository: OtpRepository,
    private val auditRepository: AuditRepository,
    @Value("\${app.name}") private val appName: String,
    @Value("\${app.secret.expiration-days:30}") private val expirationDays: Long
) {
    private val gAuth = GoogleAuthenticator()

    fun generateSecret(username: String): String {
        if (otpRepository.findByUsername(username) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User already has a secret generated")
        }
        val key = gAuth.createCredentials()
        otpRepository.save(
            OtpEntry(
                username = username,
                secret = key.key,
                expiration = LocalDateTime.now().plusDays(expirationDays)
            )
        )
        return key.key
    }

    fun generateOtpAuthUrl(secret: String, username: String): String =
        "otpauth://totp/$appName:$username?secret=$secret&issuer=$appName"

    fun generateQrCodeBase64(otpAuthUrl: String): String {
        val stream = ByteArrayOutputStream()
        QRCode.from(otpAuthUrl).withSize(250, 250).writeTo(stream)
        return Base64.getEncoder().encodeToString(stream.toByteArray())
    }

    fun validateOtp(username: String, otp: Int): Boolean {
        val entry = otpRepository.findByUsername(username)
        val isValid = entry != null && gAuth.authorize(entry.secret, otp)
        auditRepository.save(
            OtpAudit(
                username = username,
                action = if (isValid) "OTP_VALID" else "OTP_INVALID",
                timestamp = LocalDateTime.now()
            )
        )
        return isValid
    }
}