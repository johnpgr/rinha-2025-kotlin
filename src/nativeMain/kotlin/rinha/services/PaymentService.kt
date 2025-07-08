@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package rinha.services

import rinha.config.System
import rinha.database.SQLiteDatabase
import rinha.models.*
import rinha.utils.HttpClient
import kotlin.time.*
import kotlin.uuid.ExperimentalUuidApi

class PaymentService() {
    suspend fun processPayment(payment: Payment, db: SQLiteDatabase, healthCheck: HealthCheckService) {
        val processor = if (healthCheck.isDefaultHealthy()) PaymentProcessor.DEFAULT else PaymentProcessor.FALLBACK
        val url = if (processor == PaymentProcessor.DEFAULT) System.paymentApiUrl else System.paymentApiFallbackUrl

        HttpClient.postPayment(url, payment)
        db.query.insertPayment(
            payment.correlationId.toString(), payment.amount, processor.name, Clock.System.now().toString()
        )
    }
}