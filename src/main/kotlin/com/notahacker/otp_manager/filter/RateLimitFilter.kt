package com.notahacker.otp_manager.filter

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class RateLimitFilter(
    @Value("\${rate-limit.per-minute:5}") private val perMinute: Long,
    @Value("\${rate-limit.per-hour:20}") private val perHour: Long
) : Filter {

    private val cache = Caffeine.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS)
        .maximumSize(10_000)
        .build<String, Bucket>()

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        if (!httpRequest.requestURI.startsWith("/otp") || httpRequest.method == "OPTIONS") {
            chain.doFilter(request, response)
            return
        }

        val ip = resolveClientIp(httpRequest)
        val bucket = cache.get(ip) { createBucket() }!!

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response)
        } else {
            httpResponse.status = 429
            httpResponse.contentType = "application/json"
            httpResponse.writer.write("""{"status":429,"error":"Too Many Requests - slow down"}""")
        }
    }

    private fun createBucket(): Bucket = Bucket.builder()
        .addLimit(
            Bandwidth.builder()
                .capacity(perMinute)
                .refillGreedy(perMinute, Duration.ofMinutes(1))
                .build()
        )
        .addLimit(
            Bandwidth.builder()
                .capacity(perHour)
                .refillGreedy(perHour, Duration.ofHours(1))
                .build()
        )
        .build()

    private fun resolveClientIp(request: HttpServletRequest): String =
        request.getHeader("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?: request.remoteAddr
}