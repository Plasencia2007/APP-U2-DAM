package pe.upeu.ecorecicla.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import pe.upeu.ecorecicla.data.local.GestorSesion
import pe.upeu.ecorecicla.data.local.Metadatos
import pe.upeu.ecorecicla.domain.repository.Reloj
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.data.remote.dto.aDominio
import pe.upeu.ecorecicla.db.EcoDb
import pe.upeu.ecorecicla.domain.model.Resultado
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.model.map
import pe.upeu.ecorecicla.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: EcoApi,
    private val gestor: GestorSesion,
    private val db: EcoDb,
    private val reloj: Reloj,
    private val io: CoroutineDispatcher
) : AuthRepository {

    override val sesion: StateFlow<Sesion?> = gestor.sesion

    override val sesionVencida: StateFlow<Boolean> = gestor.vencida

    override suspend fun iniciarSesion(usuario: String, clave: String): Resultado<Sesion> {
        val resultado = api.login(usuario, clave).map { it.aDominio(reloj.ahoraMillis()) }
        if (resultado is Resultado.Exito) {
            withContext(io) {
                // Equipo compartido: si la base local era de otro usuario, se limpia antes de usarla.
                val duenio = db.ecoDbQueries.leerMetadato(Metadatos.USUARIO_DE_LA_BASE).executeAsOneOrNull()
                if (duenio != null && duenio != resultado.valor.usuario) {
                    db.ecoDbQueries.borrarTodo()
                }
                db.ecoDbQueries.guardarMetadato(Metadatos.USUARIO_DE_LA_BASE, resultado.valor.usuario)
            }
            gestor.guardar(resultado.valor)
        }
        return resultado
    }

    override suspend fun recuperarSesion(): Sesion? = withContext(io) { gestor.recuperar() }

    /** Cierre de sesion: borra el token del almacenamiento seguro y deja la base vacia para el siguiente turno. */
    override suspend fun cerrarSesion() {
        withContext(io) {
            gestor.borrar()
            db.ecoDbQueries.borrarTodo()
        }
    }
}
