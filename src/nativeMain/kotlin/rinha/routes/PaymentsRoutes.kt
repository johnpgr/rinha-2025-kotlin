package rinha.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import rinha.models.Payment
import rinha.services.*
import kotlin.time.*

@OptIn(ExperimentalTime::class)
fun Route.paymentsRoutes(paymentsService: PaymentsService) {

    post("/purge-payments") {
        paymentsService.purgePayments()
        call.respond(HttpStatusCode.OK, "All payments have been purged")
    }

    post("/payments") {
        val payment = call.receive<Payment>()
        paymentsService.processPayment(payment)
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

        val paymentsSummary = paymentsService.getPaymentsSummary(from, to)
        call.respond(paymentsSummary)
    }
}