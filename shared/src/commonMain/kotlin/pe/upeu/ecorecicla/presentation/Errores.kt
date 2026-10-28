package pe.upeu.ecorecicla.presentation

import pe.upeu.ecorecicla.domain.model.ErrorApi

/** Mensaje de error para la interfaz; "reintentable" decide si se ofrece el boton Reintentar. */
data class MensajeError(val texto: String, val reintentable: Boolean = false)

/**
 * Conducta de la interfaz ante cada respuesta del servicio (seccion 3.5):
 * 401 -> lo resuelve GestorSesion (borra token y vuelve al login), aqui no se muestra nada;
 * 403 -> "no autorizado" y la sesion se mantiene; 404 -> se informa y se refresca el listado;
 * 409 -> el motivo devuelto por el servidor; 5xx -> mensaje breve y reintento; sin red -> se informa.
 */
fun ErrorApi.aMensaje(): MensajeError? = when (this) {
    is ErrorApi.NoAutorizado -> null
    is ErrorApi.SinPrivilegio -> MensajeError("Operacion no autorizada: su usuario no tiene el privilegio necesario")
    is ErrorApi.NoEncontrado -> MensajeError("$mensaje. Se actualizo el listado.")
    is ErrorApi.Conflicto -> MensajeError(mensaje)
    is ErrorApi.Validacion -> MensajeError(mensaje)
    is ErrorApi.Servidor -> MensajeError("Error del servidor. Intente nuevamente.", reintentable = true)
    ErrorApi.SinConexion -> MensajeError("Sin conexion con el servidor. Se trabaja con los datos guardados en el equipo.")
    ErrorApi.TiempoAgotado -> MensajeError("El servidor tardo demasiado en responder.", reintentable = true)
    is ErrorApi.Desconocido -> MensajeError(detalle, reintentable = true)
}

/** Formatea "2026-10-28T09:14:22" como "28/10 09:14". */
fun fechaCorta(iso: String?): String {
    if (iso == null || iso.length < 16) return "nunca"
    return "${iso.substring(8, 10)}/${iso.substring(5, 7)} ${iso.substring(11, 16)}"
}

fun kg(valor: Double): String {
    val redondeado = kotlin.math.round(valor * 100) / 100
    val texto = redondeado.toString()
    return (if (texto.endsWith(".0")) texto.dropLast(2) else texto) + " kg"
}
