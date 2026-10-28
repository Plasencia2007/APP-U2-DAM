package pe.upeu.ecorecicla.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import pe.upeu.ecorecicla.data.remote.dto.CorreccionPesajeDto
import pe.upeu.ecorecicla.data.remote.dto.LoginPeticionDto
import pe.upeu.ecorecicla.data.remote.dto.LoginRespuestaDto
import pe.upeu.ecorecicla.data.remote.dto.PaginaDto
import pe.upeu.ecorecicla.data.remote.dto.PesajeDto
import pe.upeu.ecorecicla.data.remote.dto.PesajePeticionDto
import pe.upeu.ecorecicla.data.remote.dto.RutaDto
import pe.upeu.ecorecicla.data.remote.dto.UsuarioActualDto
import pe.upeu.ecorecicla.domain.model.Resultado

/** Fuente remota: cada funcion es una operacion del contrato del API y devuelve Resultado. */
class EcoApi(private val cliente: HttpClient) {

    suspend fun login(usuario: String, clave: String): Resultado<LoginRespuestaDto> = llamar(
        { cliente.post(Rutas.LOGIN) { setBody(LoginPeticionDto(usuario, clave)) } },
        { it.cuerpo() }
    )

    suspend fun usuarioActual(): Resultado<UsuarioActualDto> = llamar(
        { cliente.get(Rutas.USUARIO_ACTUAL) },
        { it.cuerpo() }
    )

    suspend fun rutaDelDia(): Resultado<RutaDto> = llamar(
        { cliente.get(Rutas.RUTA_DEL_DIA) },
        { it.cuerpo() }
    )

    suspend fun pesajesDeRuta(rutaId: Long, pagina: Int, tamanio: Int = 50): Resultado<PaginaDto<PesajeDto>> = llamar(
        {
            cliente.get(Rutas.pesajesDeRuta(rutaId)) {
                parameter("pagina", pagina)
                parameter("tamanio", tamanio)
            }
        },
        { it.cuerpo() }
    )

    suspend fun registrarPesaje(puntoId: Long, peticion: PesajePeticionDto): Resultado<PesajeDto> = llamar(
        { cliente.post(Rutas.pesajesDePunto(puntoId)) { setBody(peticion) } },
        { it.cuerpo() }
    )

    suspend fun corregirPesaje(pesajeId: Long, peticion: CorreccionPesajeDto): Resultado<PesajeDto> = llamar(
        { cliente.put(Rutas.pesaje(pesajeId)) { setBody(peticion) } },
        { it.cuerpo() }
    )

    /** 204 No Content: no se intenta convertir el cuerpo, solo se comprueba el codigo. */
    suspend fun anularPesaje(pesajeId: Long, motivo: String): Resultado<Unit> = llamar(
        { cliente.delete(Rutas.pesaje(pesajeId)) { parameter("motivo", motivo) } },
        { }
    )

    suspend fun cerrarRuta(rutaId: Long): Resultado<RutaDto> = llamar(
        { cliente.post(Rutas.cierreDeRuta(rutaId)) },
        { it.cuerpo() }
    )
}
