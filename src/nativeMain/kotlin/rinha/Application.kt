package rinha

import co.touchlab.sqliter.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.util.AttributeKey
import platform.posix.*
import rinha.config.System
import rinha.database.SQLiteDatabase
import rinha.services.HealthCheckService

fun Application.module() {
    install(ContentNegotiation) { json() }
    configureHealthCheck()
    configureDatabase()
    PaymentsApi.configureRoutes(this)
}

fun Application.configureHealthCheck() {
    val healthCheckService = HealthCheckService()
    monitor.subscribe(ApplicationStarted) { healthCheckService.start() }
    monitor.subscribe(ApplicationStopped) { healthCheckService.stop() }
    attributes.put(AttributeKey<HealthCheckService>("healthCheckService"), healthCheckService)
}

fun Application.configureDatabase() {
    val db = SQLiteDatabase("rinha.db") { config ->
        config.copy(
            journalMode = JournalMode.WAL,
            extendedConfig = config.extendedConfig.copy(
                foreignKeyConstraints = true,
                busyTimeout = 30000,
                synchronousFlag = SynchronousFlag.NORMAL,
                pageSize = 4096,
                basePath = "/app/database"
            ),
            lifecycleConfig = config.lifecycleConfig.copy(
                onCreateConnection = { connection ->
                    connection.rawExecSql("PRAGMA cache_size=-2000")
                    connection.rawExecSql("PRAGMA temp_store=MEMORY")
                }
            )
        )
    }
    attributes.put(AttributeKey<SQLiteDatabase>("db"), db)
}

fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0", module = Application::module).start(true)
}
