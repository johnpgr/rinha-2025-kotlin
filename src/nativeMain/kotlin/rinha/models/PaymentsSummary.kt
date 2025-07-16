package rinha.models

import kotlinx.serialization.Serializable

@Serializable
data class PaymentsSummary(
    val default: ProcessorSummary,
    val fallback: ProcessorSummary
)