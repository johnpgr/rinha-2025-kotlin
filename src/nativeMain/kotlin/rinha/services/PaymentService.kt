@file:OptIn(ExperimentalTime::class)

package rinha.services

import rinha.config.System
import rinha.database.SQLiteDatabase
import rinha.models.Payment
import rinha.models.PaymentProcessor
import rinha.utils.HttpClient
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PaymentService() {
    suspend fun processPayment(payment: Payment, db: SQLiteDatabase, healthCheck: HealthCheckService) {
        val processor = if (healthCheck.isDefaultHealthy()) PaymentProcessor.DEFAULT else PaymentProcessor.FALLBACK
        val url = if (processor == PaymentProcessor.DEFAULT) System.paymentApiUrl else System.paymentApiFallbackUrl

        HttpClient.postPayment(url, payment)

        db.query.insertPayment(
            payment.correlationId, payment.amount, processor.name, Clock.System.now().toEpochMilliseconds()
        )
    }
}