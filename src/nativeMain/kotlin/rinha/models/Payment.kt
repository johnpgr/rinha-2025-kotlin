@file:OptIn(ExperimentalUuidApi::class)

package rinha.models

import kotlinx.serialization.Serializable
import kotlin.uuid.*

@Serializable
data class Payment(
    val correlationId: Uuid,
    val amount: Double
)