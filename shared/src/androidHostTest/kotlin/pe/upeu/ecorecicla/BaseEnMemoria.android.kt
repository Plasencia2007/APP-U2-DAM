package pe.upeu.ecorecicla

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pe.upeu.ecorecicla.db.EcoDb

/** Controlador JDBC de SQLite en memoria: sin emulador y sin archivos. */
actual fun baseEnMemoria(): EcoDb {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    EcoDb.Schema.create(driver)
    return EcoDb(driver)
}
