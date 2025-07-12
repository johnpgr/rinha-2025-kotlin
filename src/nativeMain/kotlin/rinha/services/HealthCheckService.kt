package rinha.services

import kotlinx.coroutines.*
import rinha.models.*
import rinha.utils.HttpClient
import kotlin.time.Duration.Companion.seconds

class HealthCheckService(
    private val defaultClient: HttpClient,
    private val fallbackClient: HttpClient
) {
    private var cache = mutableMapOf<PaymentProcessor, HealthStatus>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var healthCheckJob: Job? = null

    fun start() {
        healthCheckJob = scope.launch {
            while (true) {
                try {
                    updateHealthStatus()
                    delay(5.seconds)
                } catch (e: Exception) {
                    println("Error updating health status: ${e.message}")
                    delay(5.seconds)
                }
            }
        }
    }

    fun stop() {
        healthCheckJob?.cancel()
        scope.cancel()
    }

    private suspend fun updateHealthStatus() {
        val results = awaitAll(
            scope.async { defaultClient.getHealthStatus() },
            scope.async { fallbackClient.getHealthStatus() }
        )
        cache[PaymentProcessor.DEFAULT] = results[0]
        cache[PaymentProcessor.FALLBACK] = results[1]
    }

    fun isDefaultHealthy(): Boolean {
        val status = cache[PaymentProcessor.DEFAULT] ?: return false
        return !status.failing && status.minResponseTime < 500
    }
}