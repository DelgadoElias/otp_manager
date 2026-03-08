package com.notahacker.otp_manager.exception

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<Map<String, String>> {
        val body = mapOf(
            "timestamp" to LocalDateTime.now().toString(),
            "status" to ex.statusCode.value().toString(),
            "error" to (ex.reason ?: "Unexpected error")
        )
        return ResponseEntity(body, ex.statusCode)
    }
}