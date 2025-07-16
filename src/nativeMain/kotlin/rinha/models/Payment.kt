@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class, ExperimentalSerializationApi::class)

package rinha.models

import kotlinx.serialization.*
import kotlin.time.*
import kotlin.uuid.*

@Serializable
data class Payment(
    val correlationId: Uuid,
    val amount: Double,
    @EncodeDefault
    val requestedAt: String = Clock.System.now().toString()
)