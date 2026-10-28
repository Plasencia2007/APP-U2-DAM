package pe.upeu.ecorecicla.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.usecase.IniciarSesionUseCase

data class LoginUiState(
    val usuario: String = "",
    val clave: String = "",
    val cargando: Boolean = false,
    val errorUsuario: String? = null,
    val errorClave: String? = null,
    val mensaje: String? = null
)

class LoginViewModel(private val iniciarSesion: IniciarSesionUseCase) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun cambiarUsuario(valor: String) = _estado.update { it.copy(usuario = valor, errorUsuario = null, mensaje = null) }
    fun cambiarClave(valor: String) = _estado.update { it.copy(clave = valor, errorClave = null, mensaje = null) }

    fun ingresar() {
        if (_estado.value.cargando) return
        _estado.update { it.copy(cargando = true, mensaje = null) }
        viewModelScope.launch {
            val s = _estado.value
            when (val r = iniciarSesion(s.usuario, s.clave)) {
                // La navegacion al area protegida la hace SesionViewModel al observar la sesion.
                is Resultado.Exito -> _estado.value = LoginUiState()
                is Resultado.Fallo -> _estado.update { mostrarError(it.copy(cargando = false), r.error) }
            }
        }
    }

    /** Mensajes distintos para credenciales incorrectas y para falta de conexion. */
    private fun mostrarError(s: LoginUiState, error: ErrorApi): LoginUiState = when (error) {
        is ErrorApi.Validacion -> s.copy(
            errorUsuario = error.errores["username"],
            errorClave = error.errores["password"],
            mensaje = if (error.errores.isEmpty()) error.mensaje else null
        )
        is ErrorApi.NoAutorizado -> s.copy(mensaje = "Usuario o contrasena incorrectos")
        ErrorApi.SinConexion -> s.copy(mensaje = "No hay conexion con el servidor. Verifique su red e intente de nuevo.")
        ErrorApi.TiempoAgotado -> s.copy(mensaje = "El servidor no respondio a tiempo. Intente de nuevo.")
        is ErrorApi.Servidor -> s.copy(mensaje = "El servidor tuvo un problema. Intente de nuevo.")
        else -> s.copy(mensaje = error.message)
    }
}
