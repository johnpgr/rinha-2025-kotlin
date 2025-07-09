package rinha.database

import io.github.smyrgeorge.sqlx4k.Driver
import io.github.smyrgeorge.sqlx4k.postgres.PostgreSQL
import io.github.smyrgeorge.sqlx4k.sqldelight.Sqlx4kSqldelightDriver
import rinha.AppDatabase
import rinha.config.SystemEnv

class PostgresDatabase(options: Driver.Pool.Options) {
    private val postgres = PostgreSQL(
        url = SystemEnv.dbUrl,
        username = SystemEnv.dbUsername,
        password = SystemEnv.dbPassword,
        options = options,
    )
    private val sqldelightDriver = Sqlx4kSqldelightDriver(postgres)
    private val db = AppDatabase(sqldelightDriver)
    val query = db.paymentQueries
}