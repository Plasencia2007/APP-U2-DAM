package pe.upeu.ecorecicla.domain.model

/**
 * Jerarquia sellada de errores del dominio. La capa de datos traduce cada respuesta
 * del servicio a uno de estos casos (MapeoErrores.kt) y la interfaz reacciona distinto a cada uno.
 */
sealed class ErrorApi(override val message: String) : Exception(message) {

    /** 400: validacion fallida, con el mensaje de cada campo. */
    data class Validacion(val mensaje: String, val errores: Map<String, String>) : ErrorApi(mensaje)

    /** 401: token ausente, alterado o vencido (o credenciales incorrectas en el login). */
    data class NoAutorizado(val mensaje: String = "Su sesion vencio. Inicie sesion nuevamente") : ErrorApi(mensaje)

    /** 403: autenticado pero sin el privilegio necesario. La sesion se mantiene. */
    data class SinPrivilegio(val mensaje: String = "No tiene permiso para realizar esta operacion") : ErrorApi(mensaje)

    /** 404: el recurso no existe. */
    data class NoEncontrado(val mensaje: String = "El recurso ya no existe") : ErrorApi(mensaje)

    /** 409: regla de negocio del servidor incumplida; el mensaje es el motivo. */
    data class Conflicto(val mensaje: String) : ErrorApi(mensaje)

    /** 5xx: fallo del servidor. */
    data class Servidor(val codigo: Int) : ErrorApi("El servidor no pudo completar la operacion ($codigo)")

    /** No hay red o el servicio no es alcanzable. */
    data object SinConexion : ErrorApi("Sin conexion con el servidor")

    /** La peticion supero el tiempo de espera. */
    data object TiempoAgotado : ErrorApi("El servidor tardo demasiado en responder")

    /** Cualquier otro caso no previsto. */
    data class Desconocido(val detalle: String) : ErrorApi(detalle)

    /** Errores que justifican guardar la operacion como pendiente para reenviarla luego. */
    val esDeConectividad: Boolean get() = this is SinConexion || this is TiempoAgotado
}
