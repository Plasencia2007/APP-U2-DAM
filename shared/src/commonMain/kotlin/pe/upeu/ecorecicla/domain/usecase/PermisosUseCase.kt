package pe.upeu.ecorecicla.domain.usecase

import pe.upeu.ecorecicla.domain.model.Pesaje
import pe.upeu.ecorecicla.domain.model.EstadoPesaje
import pe.upeu.ecorecicla.domain.model.Privilegios
import pe.upeu.ecorecicla.domain.model.Sesion

/**
 * Decide que acciones OFRECE la interfaz segun los privilegios de la sesion.
 * Es solo experiencia de usuario: el servicio vuelve a verificar cada privilegio y responde 403.
 * Se decide por privilegio, nunca por el nombre del rol, y fuera de los composables.
 */
class PermisosUseCase {

    fun puedeRegistrar(sesion: Sesion?): Boolean = sesion?.tiene(Privilegios.PESAJE_CREAR) == true

    fun puedeAnular(sesion: Sesion?, pesaje: Pesaje): Boolean =
        sesion?.tiene(Privilegios.PESAJE_ANULAR) == true && pesaje.estado is EstadoPesaje.Registrado

    fun puedeCerrarRuta(sesion: Sesion?): Boolean = sesion?.tiene(Privilegios.RUTA_CERRAR) == true

    /** RN-02 en la interfaz: sin PESAJE_EDITAR_TODOS solo se ofrece corregir lo propio y del dia. */
    fun puedeCorregir(sesion: Sesion?, pesaje: Pesaje, hoy: String): Boolean {
        if (sesion == null || !sesion.tiene(Privilegios.PESAJE_EDITAR)) return false
        if (pesaje.estado !is EstadoPesaje.Registrado) return false
        if (sesion.tiene(Privilegios.PESAJE_EDITAR_TODOS)) return true
        return pesaje.operario == sesion.usuario && pesaje.fechaHora.startsWith(hoy)
    }
}
