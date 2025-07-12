package rinha

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.postgres.PostgreSQL
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.util.AttributeKey
import rinha.config.Env
import rinha.models.PaymentProcessor
import rinha.services.HealthCheckService
import rinha.utils.HttpClient
import kotlin.time.Duration.Companion.minutes

fun Application.module() {
    install(ContentNegotiation) { json() }
    configureHttpClients()
    configureHealthCheck()
    configureDatabase()
    PaymentsApp.init(this)
    PaymentsApp.configureRoutes(this)
}

fun Application.configureHttpClients() {
    val defaultClient = HttpClient(Env.paymentApiUrl, PaymentProcessor.DEFAULT)
    val fallbackClient = HttpClient(Env.paymentApiFallbackUrl, PaymentProcessor.FALLBACK)
    attributes.put(AttributeKey<HttpClient>("defaultClient"), defaultClient)
    attributes.put(AttributeKey<HttpClient>("fallbackClient"), fallbackClient)
}

fun Application.configureHealthCheck() {
    val defaultClient = attributes[AttributeKey<HttpClient>("defaultClient")]
    val fallbackClient = attributes[AttributeKey<HttpClient>("fallbackClient")]
    val healthCheckService = HealthCheckService(defaultClient, fallbackClient)
    monitor.subscribe(ApplicationStarted) { healthCheckService.start() }
    monitor.subscribe(ApplicationStopped) { healthCheckService.stop() }
    attributes.put(AttributeKey<HealthCheckService>("healthCheckService"), healthCheckService)
}

fun Application.configureDatabase() {
    val options = Driver.Pool.Options.builder()
        .maxConnections(80)
        .minConnections(20)
        .maxLifetime(5.minutes)
        .build()
    val db = PostgreSQL(
        url = Env.dbUrl,
        username = Env.dbUsername,
        password = Env.dbPassword,
        options = options,
    )
    attributes.put(AttributeKey<PostgreSQL>("db"), db)
}

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module).start(true)
}
