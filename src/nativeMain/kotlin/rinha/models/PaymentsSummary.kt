package rinha.models

import kotlinx.serialization.Serializable

@Serializable
data class PaymentsSummary(
    val default: ProcessorSummary,
    val fallback: ProcessorSummary
)

@Serializable
data class ProcessorSummary(
    val totalRequests: Long,
    val totalAmount: Double
)