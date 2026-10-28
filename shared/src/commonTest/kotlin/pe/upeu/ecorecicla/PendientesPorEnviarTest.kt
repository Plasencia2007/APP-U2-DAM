package pe.upeu.ecorecicla

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import pe.upeu.ecorecicla.data.local.GestorSesion
import pe.upeu.ecorecicla.data.repository.PesajeRepositoryImpl
import pe.upeu.ecorecicla.domain.model.DatosPesaje
import pe.upeu.ecorecicla.domain.usecase.ObservarPendientesUseCase
import kotlin.test.Test
import kotlin.test.assertEquals

/** SC-D: el distintivo sale de una consulta observable de la base local, no de un contador en el ViewModel. */
class PendientesPorEnviarTest {

    @Test
    fun elConteoSigueALaBaseAlRegistrarYAlSincronizar() = runTest {
        val db = baseEnMemoria()
        val gestor = GestorSesion(AlmacenEnMemoria(), RelojFijo()).apply { guardar(sesionDePrueba()) }
        val (api, _) = apiSimulada(gestor) { respond("", HttpStatusCode.InternalServerError) }
        val repo = PesajeRepositoryImpl(db, api, RedSimulada(conectado = false), RelojFijo(), gestor, Dispatchers.Unconfined)
        val pendientes = ObservarPendientesUseCase(repo)

        assertEquals(0, pendientes().first())

        // Registrar sin red: el conteo sube sin que nadie lo incremente a mano.
        repo.registrar(6, 2, DatosPesaje("Vidrio", 31.0, 2.0, null))
        repo.registrar(6, 2, DatosPesaje("Vidrio", 32.0, 2.0, null))
        assertEquals(2, pendientes().first())

        // Lo que hace la sincronizacion al recibir el 201: limpia la marca de pendiente de la fila.
        db.ecoDbQueries.limpiarPendiente(-1)
        assertEquals(1, pendientes().first())
    }
}
