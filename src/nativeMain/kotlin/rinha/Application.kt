package rinha

import io.github.smyrgeorge.sqlx4k.Driver
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.util.AttributeKey
import rinha.database.PostgresDatabase
import rinha.services.HealthCheckService
import kotlin.time.Duration.Companion.minutes

fun Application.module() {
    install(ContentNegotiation) { json() }
    configureHealthCheck()
    configureDatabase()
    PaymentsApi.init(this)
    PaymentsApi.configureRoutes(this)
}

fun Application.configureHealthCheck() {
    val healthCheckService = HealthCheckService()
    monitor.subscribe(ApplicationStarted) { healthCheckService.start() }
    monitor.subscribe(ApplicationStopped) { healthCheckService.stop() }
    attributes.put(AttributeKey<HealthCheckService>("healthCheckService"), healthCheckService)
}

fun Application.configureDatabase() {
    val options = Driver.Pool.Options.builder()
        .maxConnections(40)
        .minConnections(10)
        .maxLifetime(10.minutes)
        .build()
    val db = PostgresDatabase(options)
    attributes.put(AttributeKey<PostgresDatabase>("db"), db)
}