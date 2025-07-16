package rinha.models

import rinha.config.Env

enum class PaymentProcessor(val url: String) {
    DEFAULT(Env.defaultProcessorURL),
    FALLBACK(Env.fallbackProcessorURL)
}

fun PaymentProcessor.cycle(): PaymentProcessor {
    return when (this) {
        PaymentProcessor.DEFAULT -> PaymentProcessor.FALLBACK
        PaymentProcessor.FALLBACK -> PaymentProcessor.DEFAULT
    }
}
