package pe.upeu.ecorecicla

import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.usecase.PermisosUseCase
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * SC-B: la app no conoce el nombre "AUDITOR". Como el auditor solo trae privilegios de lectura,
 * PermisosUseCase no le ofrece ninguna accion de escritura sin cambiar una linea de la interfaz.
 */
class PermisosAuditorTest {

    private val auditor = Sesion(
        token = "t", usuario = "auditor1", rol = "AUDITOR",
        privilegios = setOf("PUNTO_LEER", "PESAJE_LEER")
    )
    private val pesaje = Pesaje(9, 1, "2026-10-28T07:30:00", "Carton", 140.0, 10.0, 130.0, "auditor1",
        EstadoPesaje.Registrado, pendiente = false)
    private val permisos = PermisosUseCase()

    @Test
    fun auditorSoloLee() {
        assertFalse(permisos.puedeRegistrar(auditor))
        assertFalse(permisos.puedeCorregir(auditor, pesaje, "2026-10-28"))
        assertFalse(permisos.puedeAnular(auditor, pesaje))
        assertFalse(permisos.puedeCerrarRuta(auditor))
    }
}
