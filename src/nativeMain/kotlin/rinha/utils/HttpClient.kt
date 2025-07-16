@file:OptIn(ExperimentalTime::class)

package rinha.utils

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import rinha.models.*
import kotlin.time.*

object HttpClient {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun postPayment(url: String, payment: Payment): Boolean {
        val response = client.post("$url/payments") {
            contentType(ContentType.Application.Json)
            setBody(payment)
        }

        if (response.status == HttpStatusCode.InternalServerError) {
            return false
        }

        if (response.status != HttpStatusCode.OK) {
            println("Unexpected response status: ${response.status}, body: ${response.bodyAsText()}")
            throw IllegalStateException("Unexpected response status: ${response.status}")
        }

        return true
    }
}