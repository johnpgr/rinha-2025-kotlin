@file:OptIn(ExperimentalForeignApi::class)

package rinha.config

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

object Env {
    val dbUrl = (getenv("DATABASE_URL")
        ?: error("Database Environment Variable not found")).toKString()

    val dbUsername = (getenv("DATABASE_USER")
        ?: error("Database Username Environment Variable not found")).toKString()

    val dbPassword = (getenv("DATABASE_PASSWORD")
        ?: error("Database Password Environment Variable not found")).toKString()

    val paymentApiUrl = (getenv("PAYMENT_PROCESSOR_URL_DEFAULT")
        ?: error("Payment API Environment Variable not found")).toKString()

    val paymentApiFallbackUrl = (getenv("PAYMENT_PROCESSOR_URL_FALLBACK")
        ?: error("Payment API Fallback Environment Variable not found")).toKString()
}