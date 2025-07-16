@file:OptIn(ExperimentalTime::class)

package rinha.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
import rinha.database.SQLiteDatabase
import rinha.queue.PaymentQueue
import rinha.models.*
import kotlin.time.*

fun Route.paymentRoutes() {
    val db = application.attributes[AttributeKey<SQLiteDatabase>("db")]

    post("/purge-payments") {
        db.query.deleteAllPayments()
        call.respond(HttpStatusCode.OK)
    }

    post("/payments") {
        val payment = call.receive<Payment>()
        PaymentQueue.enqueue(payment)
        call.respond(HttpStatusCode.Accepted, "Payment enqueued")
    }

    get("/payments-summary") {
        // Parse as Instant just to validate these are valid timestamps
        val from = call.queryParameters["from"]?.let { Instant.parseOrNull(it) }
        val to = call.queryParameters["to"]?.let { Instant.parseOrNull(it) }

        val summaries = db.query.getPaymentsSummary(from?.toString(), to?.toString()).executeAsList()

        var defaultSummary = ProcessorSummary(0, 0.0)
        var fallbackSummary = ProcessorSummary(0, 0.0)

        summaries.forEach { (processor, totalRequests, totalAmount) ->
            when (processor) {
                PaymentProcessor.DEFAULT.name -> defaultSummary = ProcessorSummary(totalRequests, totalAmount ?: 0.0)
                PaymentProcessor.FALLBACK.name -> fallbackSummary = ProcessorSummary(totalRequests, totalAmount ?: 0.0)
                else -> error("Unknown payment processor: $processor")
            }
        }

        val paymentSummaries = PaymentsSummary(
            default = defaultSummary,
            fallback = fallbackSummary,
        )

        call.respond(HttpStatusCode.OK, paymentSummaries)
    }
}