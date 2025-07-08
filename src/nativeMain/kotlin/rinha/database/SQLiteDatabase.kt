package rinha.database

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import rinha.AppDatabase

class SQLiteDatabase(filename: String, onConfiguration: (DatabaseConfiguration) -> DatabaseConfiguration) {
    val client =
        AppDatabase(NativeSqliteDriver(schema = AppDatabase.Schema, name = filename, onConfiguration = onConfiguration))
    val query = client.databaseQueries
}