package rinha.services

import kotlinx.coroutines.*
import rinha.config.SystemEnv
import rinha.models.*
import rinha.utils.HttpClient
import kotlin.time.Duration.Companion.seconds

class HealthCheckService {
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
        PaymentProcessor.entries.forEach { processor ->
            val url = when (processor) {
                PaymentProcessor.DEFAULT -> SystemEnv.paymentApiUrl
                PaymentProcessor.FALLBACK -> SystemEnv.paymentApiFallbackUrl
            }
            val status = HttpClient.getHealthStatus("$url/payments/service-health")
            cache[processor] = status
        }
    }

    fun isDefaultHealthy(): Boolean {
        val status = cache[PaymentProcessor.DEFAULT] ?: return false
        return !status.failing && status.minResponseTime < 500
    }
}