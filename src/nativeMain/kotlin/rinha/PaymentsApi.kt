@file:OptIn(ExperimentalTime::class)

package rinha

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
import kotlinx.coroutines.sync.Semaphore
import rinha.database.SQLiteDatabase
import rinha.models.Payment
import rinha.models.*
import rinha.services.*
import kotlin.time.*

object PaymentsApi {
    private val paymentService = PaymentService()
    private lateinit var db: SQLiteDatabase
    private lateinit var healthCheckService: HealthCheckService
    private val semaphore = Semaphore(50)

    fun configureRoutes(app: Application) {
        db = app.attributes[AttributeKey<SQLiteDatabase>("db")]
        healthCheckService = app.attributes[AttributeKey<HealthCheckService>("healthCheckService")]

        app.routing {
            post("/purge-payments") {
                db.query.deleteAllPayments()
                call.respond(HttpStatusCode.OK, "All payments have been purged")
            }
            post("/payments") {
                val payment = call.receive<Payment>()

                if(!semaphore.tryAcquire()){
                    call.respond(HttpStatusCode.ServiceUnavailable, "Server is busy, please try again later")
                    return@post
                }

                try {
                    paymentService.processPayment(payment, db, healthCheckService)
                    call.respond(HttpStatusCode.Created, "Payment received")
                } finally {
                    semaphore.release()
                }
            }
            get("/payments-summary") {
                val from = call.queryParameters["from"]
                val to = call.queryParameters["to"]

                if(to != null && from != null) {
                    try {
                        Instant.parse(to)
                        Instant.parse(from)
                    } catch (_: Exception) {
                        return@get call.respond(HttpStatusCode.BadRequest, "Invalid date format for 'from' or 'to'")
                    }
                }

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