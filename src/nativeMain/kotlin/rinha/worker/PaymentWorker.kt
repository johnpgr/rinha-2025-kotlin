@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package rinha.worker

import io.ktor.server.application.Application
import io.ktor.util.AttributeKey
import kotlinx.coroutines.*
import rinha.database.SQLiteDatabase
import rinha.models.Payment
import rinha.models.PaymentProcessor
import rinha.models.cycle
import rinha.queue.PaymentQueue
import rinha.utils.HttpClient
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi

class PaymentWorker(app: Application) {
    private val db: SQLiteDatabase = app.attributes[AttributeKey<SQLiteDatabase>("db")]
    private val workerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var workerJob: Job? = null

    fun start() {
        workerJob = workerScope.launch {
            PaymentQueue.getFlow().collect { payment ->
                val processor = postPayment(payment)

                db.query.insertPayment(
                    payment.correlationId.toString(),
                    payment.amount,
                    processor.name,
                    payment.requestedAt.toString(),
                )
            }
        }
        println("Payment worker started")
    }

    fun stop() {
        workerJob?.cancel()
        workerJob = null
        println("Payment worker stopped")
    }

    private suspend fun postPayment(payment: Payment): PaymentProcessor {
        var processor = PaymentProcessor.DEFAULT

        while (!HttpClient.postPayment(processor.url, payment)) {
            processor = processor.cycle()
        }

        return processor
    }
}