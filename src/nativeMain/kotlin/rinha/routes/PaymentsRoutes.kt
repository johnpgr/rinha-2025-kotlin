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
        val from = call.queryParameters["from"]?.let { Instant.parse(it) }
        val to = call.queryParameters["to"]?.let { Instant.parse(it) }

        val summaries = db.query.getPaymentsSummary(from.toString(), to.toString()).executeAsList()

        val defaultSummary = summaries.find { it.processor == PaymentProcessor.DEFAULT.name }
            ?.let { ProcessorSummary(it.totalRequests, it.totalAmount ?: 0.0) }

        val fallbackSummary = summaries.find { it.processor == PaymentProcessor.FALLBACK.name }
            ?.let { ProcessorSummary(it.totalRequests, it.totalAmount ?: 0.0) }

        val paymentSummaries = PaymentsSummary(
            default = defaultSummary ?: ProcessorSummary(0, 0.0),
            fallback = fallbackSummary ?: ProcessorSummary(0, 0.0),
        )

        call.respond(HttpStatusCode.OK, paymentSummaries)
    }
}