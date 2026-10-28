package pe.upeu.ecorecicla.data.local

/**
 * Almacenamiento seguro de la sesion. En Android lo implementa AlmacenSeguroAndroid con
 * EncryptedSharedPreferences, cuya llave AES-256 custodia el Android Keystore.
 */
interface AlmacenSeguro {
    fun guardar(clave: String, valor: String)
    fun leer(clave: String): String?
    fun borrarTodo()
}
