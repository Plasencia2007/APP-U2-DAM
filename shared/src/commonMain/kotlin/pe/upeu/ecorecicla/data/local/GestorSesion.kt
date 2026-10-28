package pe.upeu.ecorecicla.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.upeu.ecorecicla.domain.repository.Reloj
import pe.upeu.ecorecicla.domain.model.Sesion

/**
 * Dueno unico de la sesion en memoria y de su copia en el almacenamiento seguro.
 * El cliente HTTP le pide el token para la cabecera y le avisa cuando el servicio responde 401.
 */
class GestorSesion(private val almacen: AlmacenSeguro, private val reloj: Reloj) {

    private val _sesion = MutableStateFlow<Sesion?>(null)
    val sesion: StateFlow<Sesion?> = _sesion.asStateFlow()

    /** true cuando la sesion termino por un 401 (token vencido o alterado), para avisarlo en el login. */
    private val _vencida = MutableStateFlow(false)
    val vencida: StateFlow<Boolean> = _vencida.asStateFlow()

    fun tokenActual(): String? = _sesion.value?.token

    fun guardar(sesion: Sesion) {
        almacen.guardar(TOKEN, sesion.token)
        almacen.guardar(USUARIO, sesion.usuario)
        almacen.guardar(NOMBRE, sesion.nombre)
        almacen.guardar(ROL, sesion.rol)
        almacen.guardar(PRIVILEGIOS, sesion.privilegios.joinToString(","))
        almacen.guardar(EXPIRA, sesion.expiraEnMillis.toString())
        _vencida.value = false
        _sesion.value = sesion
    }

    /** Lee la sesion guardada. Si el token ya vencio la borra y devuelve null. */
    fun recuperar(): Sesion? {
        val token = almacen.leer(TOKEN) ?: return null
        val expira = almacen.leer(EXPIRA)?.toLongOrNull() ?: 0L
        if (expira <= reloj.ahoraMillis()) {
            borrar()
            return null
        }
        val sesion = Sesion(
            token = token,
            usuario = almacen.leer(USUARIO).orEmpty(),
            rol = almacen.leer(ROL).orEmpty(),
            privilegios = almacen.leer(PRIVILEGIOS).orEmpty().split(",").filter { it.isNotBlank() }.toSet(),
            nombre = almacen.leer(NOMBRE).orEmpty(),
            expiraEnMillis = expira
        )
        _sesion.value = sesion
        return sesion
    }

    /** Borra el token del almacenamiento seguro y la sesion en memoria. */
    fun borrar() {
        almacen.borrarTodo()
        _sesion.value = null
    }

    /** Llamado por el cliente HTTP ante un 401: se borra el token y se vuelve al login. */
    fun invalidarPor401() {
        if (_sesion.value != null) _vencida.value = true
        borrar()
    }

    private companion object {
        const val TOKEN = "token"
        const val USUARIO = "usuario"
        const val NOMBRE = "nombre"
        const val ROL = "rol"
        const val PRIVILEGIOS = "privilegios"
        const val EXPIRA = "expira"
    }
}
