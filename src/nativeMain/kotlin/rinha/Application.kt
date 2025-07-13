package rinha

import io.github.domgew.kedis.KedisClient
import io.github.domgew.kedis.KedisConfiguration
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.postgres.PostgreSQL
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.*
import io.ktor.util.AttributeKey
import rinha.config.Env
import rinha.models.PaymentProcessor
import rinha.routes.paymentsRoutes
import rinha.services.HealthCheckService
import rinha.services.PaymentsService
import rinha.utils.HttpClient
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

fun Application.module() {
    install(ContentNegotiation) { json() }
    configureHttpClients()
    configureRedis()
    configureServices()
    configureRouting()
}

fun Application.configureServices() {
    val defaultClient = attributes[AttributeKey<HttpClient>("defaultClient")]
    val fallbackClient = attributes[AttributeKey<HttpClient>("fallbackClient")]
    val db = attributes[AttributeKey<PostgreSQL>("db")]

    val healthCheckService = HealthCheckService(defaultClient, fallbackClient)
    attributes.put(AttributeKey<HealthCheckService>("healthCheckService"), healthCheckService)
    monitor.subscribe(ApplicationStarted) { healthCheckService.start() }
    monitor.subscribe(ApplicationStopped) { healthCheckService.stop() }

    val paymentsService = PaymentsService(db, healthCheckService, defaultClient, fallbackClient)
    attributes.put(AttributeKey<PaymentsService>("paymentService"), paymentsService)
}

fun Application.configureRouting() {
    val paymentsService = attributes[AttributeKey<PaymentsService>("paymentService")]

    routing {
        paymentsRoutes(paymentsService)
    }
}

fun Application.configureHttpClients() {
    val defaultClient = HttpClient(Env.paymentApiUrl, PaymentProcessor.DEFAULT)
    val fallbackClient = HttpClient(Env.paymentApiFallbackUrl, PaymentProcessor.FALLBACK)
    attributes.put(AttributeKey<HttpClient>("defaultClient"), defaultClient)
    attributes.put(AttributeKey<HttpClient>("fallbackClient"), fallbackClient)
}

fun Application.configureRedis() {
    val redis = KedisClient(
        configuration = KedisConfiguration(
            endpoint = KedisConfiguration.Endpoint.HostPort(
                host = "0.0.0.0",
                port = 6379,
            ),
            authentication = KedisConfiguration.Authentication.AutoAuth(
                password = "secret",
                username = "admin",
            ),
            connectionTimeout = 250.milliseconds,
            keepAlive = true,
            databaseIndex = 1,
        ),
    )
    attributes.put(AttributeKey<KedisClient>("redis"), redis)
}

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module).start(true)
}
