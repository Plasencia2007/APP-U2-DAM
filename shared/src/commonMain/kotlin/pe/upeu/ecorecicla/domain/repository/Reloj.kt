package pe.upeu.ecorecicla.domain.repository

/** Hora del dispositivo. Se inyecta para poder fijarla en las pruebas. */
interface Reloj {
    fun ahoraMillis(): Long

    /** Fecha y hora local en ISO-8601 sin zona, igual que el servicio: 2026-10-27T09:14:22. */
    fun ahoraIso(): String

    /** Fecha local en ISO-8601: 2026-10-27. */
    fun hoy(): String = ahoraIso().substring(0, 10)
}
