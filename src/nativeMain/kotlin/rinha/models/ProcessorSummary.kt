package rinha.models

import kotlinx.serialization.Serializable

@Serializable
data class ProcessorSummary(
    val totalRequests: Long,
    val totalAmount: Double
)
