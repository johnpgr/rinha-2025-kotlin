@file:OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)

package rinha.services

import io.github.domgew.kedis.KedisClient
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import rinha.models.*
import rinha.utils.HttpClient
import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi

class PaymentsService(
    private val redis: KedisClient,
    private val healthCheckService: HealthCheckService,
    private val defaultHttpClient: HttpClient,
    private val fallbackHttpClient: HttpClient
) {
    companion object {
        const val deleteAllQuery = "DELETE FROM Payment;"

        val insertPaymentStmt = Statement.create(
            """
                INSERT INTO Payment(correlation_id, amount, processor)
                VALUES(:id, :amount, :processor);
            """.trimIndent()
        )

        val getPaymentsSummaryStmt = Statement.create(
            """
                SELECT processor, COUNT(*) AS total_requests, SUM(amount) AS total_amount
                FROM Payment
                WHERE (:from IS NULL OR timestamp >= :from) AND (:to IS NULL OR timestamp <= :to)
                GROUP BY processor;
            """.trimIndent()
        )
    }

    data class PaymentRecord(
        val correlationId: String,
        val amount: Double,
        val processor: String,
    )

    private val paymentChannel = Channel<PaymentRecord>(Channel.UNLIMITED)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        repeat(4) {
            scope.launch {
                processBatchInserts()
            }
        }
    }

    private suspend fun processBatchInserts() {
        val batch = mutableListOf<PaymentRecord>()

        while (true) {
            try {
                batch.add(paymentChannel.receive())

                withTimeoutOrNull(5.milliseconds) {
                    while (batch.size < 100) {
                        batch.add(paymentChannel.receive())
                    }
                }

                if (batch.isNotEmpty()) {
                    insertBatch(batch.toList())
                    batch.clear()
                }
            } catch (e: Exception) {
                batch.clear()
                delay(1.milliseconds)
            }
        }
    }

    private suspend fun insertBatch(batch: List<PaymentRecord>) {
        db.transaction {
            batch.forEach { payment ->
                execute(
                    insertPaymentStmt.bind("id", payment.correlationId).bind("amount", payment.amount)
                        .bind("processor", payment.processor),
                )
            }
        }
    }

    suspend fun getPaymentsSummary(from: String?, to: String?): PaymentsSummary {
        var defaultSummary: ProcessorSummary? = null
        var fallbackSummary: ProcessorSummary? = null

        db.fetchAll(
            getPaymentsSummaryStmt.bind("from", from).bind("to", to)
        ).getOrThrow().map { row ->
            val processor = row.get("processor").asString()
            val totalRequests = row.get("total_requests").asLong()
            val totalAmount = row.get("total_amount").asDouble()
            when (processor) {
                PaymentProcessor.DEFAULT.name -> {
                    defaultSummary = ProcessorSummary(totalRequests.toInt(), totalAmount)
                }

                PaymentProcessor.FALLBACK.name -> {
                    fallbackSummary = ProcessorSummary(totalRequests.toInt(), totalAmount)
                }

                else -> throw IllegalArgumentException("Unknown processor: $processor")
            }
        }

        return PaymentsSummary(
            default = defaultSummary ?: ProcessorSummary(0, 0.0), fallback = fallbackSummary ?: ProcessorSummary(0, 0.0)
        )
    }

    suspend fun processPayment(payment: Payment) {
        val client = if (healthCheckService.isDefaultHealthy()) defaultHttpClient else fallbackHttpClient

        coroutineScope {
            awaitAll(
                async { client.postPayment(payment) },
                async {
                    paymentChannel.send(
                        PaymentRecord(
                            correlationId = payment.correlationId.toString(),
                            amount = payment.amount,
                            processor = client.processor.name,
                        )
                    )
                }
            )
        }
    }

    suspend fun purgePayments() {
        db.execute(deleteAllQuery)
    }
}