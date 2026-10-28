package pe.upeu.ecorecicla

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import pe.upeu.ecorecicla.data.local.AlmacenSeguro

/**
 * Preferencias cifradas respaldadas por el almacen de claves de Android (Android Keystore).
 * Claves y valores se cifran con una llave AES-256 que nunca sale del Keystore, por eso el archivo
 * "sesion_ecorutas.xml" no contiene el token en claro aunque se extraiga del telefono.
 */
@Suppress("DEPRECATION")
class AlmacenSeguroAndroid(contexto: Context) : AlmacenSeguro {

    private val llaveMaestra = MasterKey.Builder(contexto)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferencias = EncryptedSharedPreferences.create(
        contexto,
        "sesion_ecorutas",
        llaveMaestra,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun guardar(clave: String, valor: String) {
        preferencias.edit().putString(clave, valor).apply()
    }

    override fun leer(clave: String): String? = preferencias.getString(clave, null)

    override fun borrarTodo() {
        preferencias.edit().clear().apply()
    }
}
