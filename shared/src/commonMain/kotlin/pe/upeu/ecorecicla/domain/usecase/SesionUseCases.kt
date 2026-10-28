package pe.upeu.ecorecicla.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import pe.upeu.ecorecicla.domain.model.ErrorApi
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.AuthRepository

class IniciarSesionUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(usuario: String, clave: String): Resultado<Sesion> {
        val errores = buildMap {
            if (usuario.isBlank()) put("username", "Ingrese su usuario")
            if (clave.isBlank()) put("password", "Ingrese su contrasena")
        }
        if (errores.isNotEmpty()) return Resultado.Fallo(ErrorApi.Validacion("Datos incompletos", errores))
        return auth.iniciarSesion(usuario.trim(), clave)
    }
}

class RecuperarSesionUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke(): Sesion? = auth.recuperarSesion()
}

class ObservarSesionUseCase(private val auth: AuthRepository) {
    operator fun invoke(): StateFlow<Sesion?> = auth.sesion

    /** true si la sesion termino por un 401, para avisarlo en el inicio de sesion. */
    fun vencida(): StateFlow<Boolean> = auth.sesionVencida
}

class CerrarSesionUseCase(private val auth: AuthRepository) {
    suspend operator fun invoke() = auth.cerrarSesion()
}
