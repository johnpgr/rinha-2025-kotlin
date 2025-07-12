@file:OptIn(ExperimentalTime::class)

package rinha.utils

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import rinha.models.*
import kotlin.time.*

class HttpClient(
    private val baseUrl: String,
    val processor: PaymentProcessor
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun postPayment(payment: Payment) {
        client.post("$baseUrl/payments") {
            contentType(ContentType.Application.Json)
            setBody(payment)
        }
    }

    suspend fun getHealthStatus(): HealthStatus {
        val response = client.get("$baseUrl/payments/service-health")
        val json = Json.parseToJsonElement(response.bodyAsText())
        val failing = json.jsonObject["failing"]?.jsonPrimitive?.boolean
        val minResponseTime = json.jsonObject["minResponseTime"]?.jsonPrimitive?.int

        return HealthStatus(
            failing = failing ?: true,
            minResponseTime =  minResponseTime ?: 9999,
            lastChecked = Clock.System.now().toEpochMilliseconds()
        )
    }
}
