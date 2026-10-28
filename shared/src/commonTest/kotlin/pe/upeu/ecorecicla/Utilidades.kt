package pe.upeu.ecorecicla

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import kotlinx.coroutines.flow.MutableStateFlow
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.local.GestorSesion
import pe.upeu.ecorecicla.data.remote.ApiConfig
import pe.upeu.ecorecicla.data.remote.EcoApi
import pe.upeu.ecorecicla.data.remote.crearClienteHttp
import pe.upeu.ecorecicla.db.EcoDb
import pe.upeu.ecorecicla.domain.model.Sesion
import pe.upeu.ecorecicla.domain.repository.MonitorDeRed
import pe.upeu.ecorecicla.domain.repository.Reloj

/** Base SQLite EN MEMORIA nueva para cada prueba (cada plataforma de pruebas aporta su driver). */
expect fun baseEnMemoria(): EcoDb

class RelojFijo(var iso: String = "2026-10-27T09:00:00", var millis: Long = 1_000_000L) : Reloj {
    override fun ahoraMillis(): Long = millis
    override fun ahoraIso(): String = iso
}

class AlmacenEnMemoria : AlmacenSeguro {
    val datos = mutableMapOf<String, String>()
    override fun guardar(clave: String, valor: String) { datos[clave] = valor }
    override fun leer(clave: String): String? = datos[clave]
    override fun borrarTodo() = datos.clear()
}

class RedSimulada(conectado: Boolean) : MonitorDeRed {
    override val conectado = MutableStateFlow(conectado)
}

/** Arma la API real sobre el motor HTTP SIMULADO (MockEngine): ninguna prueba toca la red. */
fun apiSimulada(
    gestor: GestorSesion = GestorSesion(AlmacenEnMemoria(), RelojFijo()),
    manejador: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
): Pair<EcoApi, MockEngine> {
    val motor = MockEngine(manejador)
    val cliente = crearClienteHttp(
        motor = motor,
        config = ApiConfig(urlBase = "http://eco.test/api/v1/", registroDetallado = false),
        tokenActual = { gestor.tokenActual() },
        al401 = { gestor.invalidarPor401() }
    )
    return EcoApi(cliente) to motor
}

fun sesionDePrueba(usuario: String = "operario1") = Sesion(
    token = "token-de-prueba",
    usuario = usuario,
    rol = "OPERARIO",
    privilegios = setOf("PUNTO_LEER", "PESAJE_LEER", "PESAJE_CREAR", "PESAJE_EDITAR"),
    nombre = "Operario",
    expiraEnMillis = 9_999_999L
)
