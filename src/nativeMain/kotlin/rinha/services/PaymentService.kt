@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package rinha.services

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import rinha.GetPaymentsSummary
import rinha.config.SystemEnv
import rinha.database.PostgresDatabase
import rinha.models.*
import rinha.utils.HttpClient
import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi

class PaymentService(private val db: PostgresDatabase, private val healthCheckService: HealthCheckService) {
    data class PaymentRecord(
        val correlationId: String, val amount: Double, val processor: String, val timestamp: String
    )

    private val paymentChannel = Channel<PaymentRecord>(Channel.UNLIMITED)
    private val writeSemaphore = Semaphore(1)
    private val readSemaphore = Semaphore(10)
    private val ioDispatcher = Dispatchers.IO.limitedParallelism(20)

    init {
        CoroutineScope(Dispatchers.Default).launch {
            processBatchInserts()
        }
    }

    private suspend fun processBatchInserts() {
        val batch = mutableListOf<PaymentRecord>()

        while (true) {
            try {
                withTimeoutOrNull(10.milliseconds) {
                    repeat(50) {
                        val record = paymentChannel.tryReceive().getOrThrow()
                        batch.add(record)
                    }
                }

                if (batch.isNotEmpty()) {
                    insertBatch(batch.toList())
                    batch.clear()
                }
            } catch (e: Exception) {
                batch.clear()
            }
        }
    }

    private suspend fun insertBatch(batch: List<PaymentRecord>) {
        writeSemaphore.withPermit {
            withContext(ioDispatcher) {
                batch.forEach { record ->
                    db.query.transaction(false) {
                        db.query.insertPayment(record.correlationId, record.amount, record.processor)
                    }
                }
            }
        }
    }

    suspend fun getPaymentsSummary(from: String?, to: String?): Flow<Query<GetPaymentsSummary>> {
        readSemaphore.withPermit {
            return db.query.getPaymentsSummary(from, to).asFlow()
        }
    }

    suspend fun processPayment(payment: Payment) {
        val processor =
            if (healthCheckService.isDefaultHealthy()) PaymentProcessor.DEFAULT else PaymentProcessor.FALLBACK
        val url =
            if (processor == PaymentProcessor.DEFAULT) SystemEnv.paymentApiUrl else SystemEnv.paymentApiFallbackUrl

        HttpClient.postPayment(url, payment)
        db.query.insertPayment(
            payment.correlationId.toString(), payment.amount, processor.name,
        )
    }
}