package pe.upeu.ecorecicla

import pe.upeu.ecorecicla.presentation.fechaCorta
import pe.upeu.ecorecicla.presentation.fechaLarga
import pe.upeu.ecorecicla.presentation.numeroKg
import kotlin.test.Test
import kotlin.test.assertEquals

/** Formatos que ve el operario: kilos con coma decimal y fechas legibles. */
class FormatoTest {

    @Test
    fun kilosConComaDecimalYSinCerosSobrantes() {
        assertEquals("45", numeroKg(45.0))
        assertEquals("45,5", numeroKg(45.5))
        assertEquals("568,5", numeroKg(568.5))
        assertEquals("1,33", numeroKg(1.333))
    }

    @Test
    fun fechasLegibles() {
        assertEquals("28 de octubre", fechaLarga("2026-10-28"))
        assertEquals("hoy 09:14", fechaCorta("2026-10-28T09:14:22", hoy = "2026-10-28"))
        assertEquals("27/10 18:05", fechaCorta("2026-10-27T18:05:00", hoy = "2026-10-28"))
        assertEquals("nunca", fechaCorta(null))
    }
}
