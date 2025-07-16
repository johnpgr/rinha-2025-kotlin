@file:OptIn(ExperimentalForeignApi::class)

package rinha.config

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

object Env {
    val defaultProcessorURL = (getenv("PAYMENT_PROCESSOR_URL_DEFAULT")
        ?: error("Payment API Environment Variable not found")).toKString()

    val fallbackProcessorURL = (getenv("PAYMENT_PROCESSOR_URL_FALLBACK")
        ?: error("Payment API Fallback Environment Variable not found")).toKString()
}