package com.notahacker.otp_manager.controller

import com.notahacker.otp_manager.service.OtpService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/otp")
class OtpController(private val otpService: OtpService) {

    @PostMapping("/generate-secret")
    fun generateSecret(@RequestParam username: String): Map<String, String> {
        val secret = otpService.generateSecret(username)
        val otpAuthUrl = otpService.generateOtpAuthUrl(secret, username)
        val qrBase64 = otpService.generateQrCodeBase64(otpAuthUrl)
        return mapOf("secret" to secret, "qrCode" to "data:image/png;base64,$qrBase64")
    }

    @PostMapping("/validate")
    fun validateOtp(@RequestParam username: String, @RequestParam otp: Int): Map<String, Boolean> {
        return mapOf("valid" to otpService.validateOtp(username, otp))
    }
}