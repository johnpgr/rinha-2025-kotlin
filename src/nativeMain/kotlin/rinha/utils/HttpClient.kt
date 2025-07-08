@file:OptIn(ExperimentalTime::class)

package rinha.utils

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import rinha.models.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object HttpClient {
    private val client = HttpClient()

    suspend fun postPayment(url: String, payment: Payment) {
        client.post("$url/payments") {
            contentType(ContentType.Application.Json)
            setBody(payment)
        }
    }

    suspend fun getHealthStatus(url: String): HealthStatus {
        val response = client.get(url)
        val json = Json.parseToJsonElement(response.bodyAsText())
        return HealthStatus(
            failing = json.jsonObject["failing"]?.jsonPrimitive?.boolean ?: true,
            minResponseTime = json.jsonObject["minResponseTime"]?.jsonPrimitive?.int ?: 9999,
            lastChecked = Clock.System.now().toEpochMilliseconds()
        )
    }
}