@file:OptIn(ExperimentalTime::class)

package rinha

import io.github.smyrgeorge.sqlx4k.postgres.PostgreSQL
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
import rinha.models.Payment
import rinha.services.*
import rinha.utils.HttpClient
import kotlin.time.*

object PaymentsApp {
    private lateinit var paymentService: PaymentService
    private lateinit var healthCheckService: HealthCheckService

    fun init(app: Application) {
        val db = app.attributes[AttributeKey<PostgreSQL>("db")]
        val defaultClient = app.attributes[AttributeKey<HttpClient>("defaultClient")]
        val fallbackClient = app.attributes[AttributeKey<HttpClient>("fallbackClient")]

        healthCheckService = app.attributes[AttributeKey<HealthCheckService>("healthCheckService")]
        paymentService = PaymentService(db, healthCheckService, defaultClient, fallbackClient)
    }

    fun configureRoutes(app: Application) {
        app.routing {
            post("/purge-payments") {
                paymentService.purgePayments()
                call.respond(HttpStatusCode.OK, "All payments have been purged")
            }

            post("/payments") {
                val payment = call.receive<Payment>()
                paymentService.processPayment(payment)
                call.respond(HttpStatusCode.Created, "Payment received")
            }

            get("/payments-summary") {
                val from = call.queryParameters["from"]
                val to = call.queryParameters["to"]

                if (from != null || to != null) {
                    try {
                        from?.let { Instant.parse(it) }
                        to?.let { Instant.parse(it) }
                    } catch (_: Exception) {
                        return@get call.respond(HttpStatusCode.BadRequest, "Invalid date format")
                    }
                }

                val paymentsSummary = paymentService.getPaymentsSummary(from, to)
                call.respond(paymentsSummary)
            }
        }
    }
}

