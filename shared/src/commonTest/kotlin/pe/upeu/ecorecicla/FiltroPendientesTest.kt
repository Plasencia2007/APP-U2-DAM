package pe.upeu.ecorecicla

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.upeu.ecorecicla.data.repository.RutaRepositoryImpl
import pe.upeu.ecorecicla.domain.usecase.ObservarRutaUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

/** SC-A: el filtro "Solo pendientes de pesar" se resuelve en la consulta local y se combina con el buscador. */
class FiltroPendientesTest {

    @Test
    fun soloPendientesSeCombinaConElBuscadorSinRed() = runTest {
        val db = baseEnMemoria()
        val q = db.ecoDbQueries
        q.insertarRuta(2, "2026-10-28", "ABIERTA", "operario1")
        q.insertarPunto(1, 2, "Supermercados del Norte", "Av. 1", "Norte", "Carton", 1, false)
        q.insertarPunto(2, 2, "Textiles Andinos", "Jr. 2", "Norte", "Plastico", 2, false)
        q.insertarPunto(3, 2, "Hotel Plaza", "Jr. 3", "Centro", "Vidrio", 3, false)
        // El punto 1 ya tiene un pesaje vigente; el 2 tiene uno anulado (sigue pendiente).
        q.insertarPesaje(10, 1, 2, "2026-10-28T08:00:00", "Carton", 50.0, 5.0, 45.0, "operario1",
            null, "REGISTRADO", null, false, null, null, null)
        q.insertarPesaje(11, 2, 2, "2026-10-28T08:10:00", "Plastico", 20.0, 2.0, 18.0, "operario1",
            null, "ANULADO", "Duplicado", false, null, null, null)

        // La API nunca se llama: todo sale de la base local (funciona sin conexion).
        val (api, _) = apiSimulada { respond("", HttpStatusCode.InternalServerError) }
        val observar = ObservarRutaUseCase(RutaRepositoryImpl(db, api, Dispatchers.Unconfined))

        assertEquals(listOf(1L, 2L, 3L), observar("", soloPendientes = false).first()!!.puntos.map { it.id })
        assertEquals(listOf(2L, 3L), observar("", soloPendientes = true).first()!!.puntos.map { it.id })
        assertEquals(listOf(2L), observar("Norte", soloPendientes = true).first()!!.puntos.map { it.id })
        // El avance de la cabecera no depende del filtro.
        assertEquals(3, observar("Norte", soloPendientes = true).first()!!.avance.size)
    }
}
