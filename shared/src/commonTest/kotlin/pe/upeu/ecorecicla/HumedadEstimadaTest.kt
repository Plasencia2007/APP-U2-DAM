package pe.upeu.ecorecicla

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import pe.upeu.ecorecicla.data.local.GestorSesion
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.aDominio
import pe.upeu.ecorecicla.data.remote.jsonEco
import pe.upeu.ecorecicla.data.repository.PesajeRepositoryImpl
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.usecase.RegistrarPesajeUseCase
import pe.upeu.ecorecicla.domain.usecase.ValidadorPesaje
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** SC-C: la humedad estimada recorre DTO, dominio, validacion y base local. */
class HumedadEstimadaTest {

    @Test
    fun elDtoConvierteLaHumedadAlDominio() {
        val dto = jsonEco.decodeFromString(PesajeDto.serializer(), """{"id":17,"puntoId":5,"rutaId":2,
            "fechaHora":"2026-10-28T16:10:00","material":"Vidrio","pesoBruto":80,"tara":6,"pesoNeto":74,
            "operario":"operario1","estado":"REGISTRADO","humedadEstimada":2.0}""")
        assertEquals(2.0, dto.aDominio().humedadEstimada)
    }

    @Test
    fun laHumedadFueraDeRangoSeRechazaBajoSuCampo() {
        val (datos, errores) = ValidadorPesaje.validar("Vidrio", "80", "6", "140")
        assertNull(datos)
        assertEquals("La humedad debe estar entre 0 y 100 %", errores["humedadEstimada"])
        assertNull(ValidadorPesaje.validar("Vidrio", "80", "6", "").first!!.humedadEstimada)
    }

    @Test
    fun sinConexionLaHumedadQuedaGuardadaEnLaFilaPendiente() = runTest {
        val db = baseEnMemoria()
        val gestor = GestorSesion(AlmacenEnMemoria(), RelojFijo()).apply { guardar(sesionDePrueba()) }
        val (api, _) = apiSimulada(gestor) { respond("", HttpStatusCode.InternalServerError) }
        val repo = PesajeRepositoryImpl(db, api, RedSimulada(conectado = false), RelojFijo(), gestor, Dispatchers.Unconfined)

        val r = RegistrarPesajeUseCase(repo)(5, 2, "Vidrio", "80", "6", "", humedad = "12,5")

        assertIs<Resultado.Exito<*>>(r)
        assertEquals(12.5, db.ecoDbQueries.pesajePorId(-1).executeAsOne().humedad_estimada)
    }
}
