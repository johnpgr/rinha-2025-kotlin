package rinha.models

data class HealthStatus(
    val failing: Boolean,
    val minResponseTime: Int,
    val lastChecked: Long
)