package rinha.queue

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import rinha.models.Payment

object PaymentQueue {
    private val channel: Channel<Payment> = Channel(capacity = Channel.UNLIMITED)

    suspend fun enqueue(payment: Payment) {
        channel.send(payment)
    }

    fun getFlow() = channel.receiveAsFlow()
}