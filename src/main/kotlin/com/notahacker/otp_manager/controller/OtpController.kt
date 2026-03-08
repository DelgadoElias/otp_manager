package com.notahacker.otp_manager.controller

import com.notahacker.otp_manager.service.OtpService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/otp")
@Tag(name = "OTP", description = "Generate and validate TOTP secrets")
class OtpController(private val otpService: OtpService) {

    @Operation(
        summary = "Generate a TOTP secret for a user",
        description = "Creates a new TOTP secret and returns it along with a QR code (base64 PNG) ready to scan with any authenticator app.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Secret generated successfully",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(value = """{"secret":"JBSWY3DPEHPK3PXP","qrCode":"data:image/png;base64,..."}""")]
                )]
            ),
            ApiResponse(responseCode = "409", description = "User already has a secret generated")
        ]
    )
    @PostMapping("/generate-secret")
    fun generateSecret(
        @Parameter(description = "Unique identifier for the user (e.g. email)", required = true)
        @RequestParam username: String
    ): Map<String, String> {
        val secret = otpService.generateSecret(username)
        val otpAuthUrl = otpService.generateOtpAuthUrl(secret, username)
        val qrBase64 = otpService.generateQrCodeBase64(otpAuthUrl)
        return mapOf("secret" to secret, "qrCode" to "data:image/png;base64,$qrBase64")
    }

    @Operation(
        summary = "Validate a TOTP code",
        description = "Validates a 6-digit TOTP code against the stored secret for the given user.",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Validation result",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(example = """{"valid":true}""")
                )]
            )
        ]
    )
    @PostMapping("/validate")
    fun validateOtp(
        @Parameter(description = "User identifier", required = true)
        @RequestParam username: String,
        @Parameter(description = "6-digit TOTP code", required = true, example = "123456")
        @RequestParam otp: Int
    ): Map<String, Boolean> {
        return mapOf("valid" to otpService.validateOtp(username, otp))
    }
}
