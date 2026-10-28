package pe.upeu.ecorecicla.data.local

import app.cash.sqldelight.db.SqlDriver
import pe.upeu.ecorecicla.db.EcoDb

/** Fabrica del controlador de SQLDelight. Android aporta BaseLocalAndroid; las pruebas, uno en memoria. */
fun interface BaseLocal {
    fun crearDriver(): SqlDriver
}

fun BaseLocal.crearBase(): EcoDb = EcoDb(crearDriver())
