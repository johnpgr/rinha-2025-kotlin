@file:OptIn(ExperimentalTime::class)

package rinha

import app.cash.sqldelight.coroutines.mapToList
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
import rinha.database.PostgresDatabase
import rinha.models.Payment
import rinha.models.*
import rinha.services.*
import kotlin.time.*

object PaymentsApi {
    private lateinit var paymentService: PaymentService
    private lateinit var db: PostgresDatabase
    private lateinit var healthCheckService: HealthCheckService

    fun init(app: Application) {
        db = app.attributes[AttributeKey<PostgresDatabase>("db")]
        healthCheckService = app.attributes[AttributeKey<HealthCheckService>("healthCheckService")]
        paymentService = PaymentService(db, healthCheckService)
    }

    fun configureRoutes(app: Application) {
        app.routing {
            post("/purge-payments") {
                db.query.deleteAllPayments()
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

                paymentService.getPaymentsSummary(from, to).mapToList(app.coroutineContext).collect { summaries ->
                    val defaultSummary = summaries.find { it.processor == PaymentProcessor.DEFAULT.name }?.let {
                        ProcessorSummary(it.totalRequests.toInt(), it.totalAmount ?: 0.0)
                    } ?: ProcessorSummary(0, 0.0)
                    val fallbackSummary = summaries.find { it.processor == PaymentProcessor.FALLBACK.name }?.let {
                        ProcessorSummary(it.totalRequests.toInt(), it.totalAmount ?: 0.0)
                    } ?: ProcessorSummary(0, 0.0)
                    val paymentsSummary = PaymentsSummary(defaultSummary, fallbackSummary)
                    call.respond(paymentsSummary)
                }
            }
        }
    }
}