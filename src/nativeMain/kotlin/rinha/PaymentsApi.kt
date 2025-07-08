package rinha

import app.cash.sqldelight.Query
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
import kotlinx.coroutines.*
import rinha.database.SQLiteDatabase
import rinha.models.Payment
import rinha.models.PaymentProcessor
import rinha.models.PaymentsSummary
import rinha.models.ProcessorSummary
import rinha.services.HealthCheckService
import rinha.services.PaymentService

object PaymentsApi {
    private val paymentService = PaymentService()
    private lateinit var db: SQLiteDatabase
    private lateinit var healthCheckService: HealthCheckService

    fun configureRoutes(app: Application) {
        db = app.attributes[AttributeKey<SQLiteDatabase>("db")]
        healthCheckService = app.attributes[AttributeKey<HealthCheckService>("healthCheckService")]

        app.routing {
            post("/payments") {
                val payment = call.receive<Payment>()
                call.respond(HttpStatusCode.Accepted)
                coroutineScope {
                    launch {
                        paymentService.processPayment(payment, db, healthCheckService)
                    }
                }
            }
            get("/payments-summary") {
                val from = call.parameters["from"]?.toLongOrNull()
                val to = call.parameters["to"]?.toLongOrNull()
                val summaryData = db.query.getPaymentsSummary(from, to).executeAsList()

                val defaultSummary = summaryData.find { it.processor == PaymentProcessor.DEFAULT.name }?.let {
                    ProcessorSummary(it.totalRequests.toInt(), it.totalAmount ?: 0.0)
                } ?: ProcessorSummary(0, 0.0)
                val fallbackSummary = summaryData.find { it.processor == PaymentProcessor.FALLBACK.name }?.let {
                    ProcessorSummary(it.totalRequests.toInt(), it.totalAmount ?: 0.0)
                } ?: ProcessorSummary(0, 0.0)
                val paymentsSummary = PaymentsSummary(defaultSummary, fallbackSummary)

                call.respond(paymentsSummary)
            }
        }
    }
}