package pe.upeu.ecorecicla

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.upeu.ecorecicla.domain.repository.MonitorDeRed

/** Observa la conectividad del telefono; al volver la senal la app sincroniza sola. */
class MonitorDeRedAndroid(contexto: Context) : MonitorDeRed {

    private val gestor = contexto.getSystemService(ConnectivityManager::class.java)
    private val redesActivas = mutableSetOf<Network>()
    private val _conectado = MutableStateFlow(estadoInicial())
    override val conectado: StateFlow<Boolean> = _conectado.asStateFlow()

    init {
        val peticion = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        gestor.registerNetworkCallback(peticion, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                synchronized(redesActivas) { redesActivas += network }
                _conectado.value = true
            }

            override fun onLost(network: Network) {
                val quedan = synchronized(redesActivas) { redesActivas -= network; redesActivas.isNotEmpty() }
                _conectado.value = quedan
            }
        })
    }

    private fun estadoInicial(): Boolean {
        val capacidades = gestor.getNetworkCapabilities(gestor.activeNetwork) ?: return false
        return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
