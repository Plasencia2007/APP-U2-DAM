package pe.upeu.ecorecicla

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pe.upeu.ecorecicla.data.local.AlmacenSeguro
import pe.upeu.ecorecicla.data.local.BaseLocal
import pe.upeu.ecorecicla.data.remote.ApiConfig
import pe.upeu.ecorecicla.domain.repository.MonitorDeRed
import pe.upeu.ecorecicla.domain.repository.Reloj
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Dependencias propias de Android: motor HTTP, almacen seguro, driver de SQLite, red y reloj. */
val platformModule = module {
    single<HttpClientEngine> { OkHttp.create() }
    single {
        ApiConfig(
            urlBase = BuildConfig.URL_BASE,
            registroDetallado = BuildConfig.REGISTRO_DETALLADO
        )
    }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<BaseLocal> { BaseLocalAndroid(androidContext()) }
    single<MonitorDeRed> { MonitorDeRedAndroid(androidContext()) }
    single<Reloj> { RelojAndroid }
}

object RelojAndroid : Reloj {
    override fun ahoraMillis(): Long = System.currentTimeMillis()
    override fun ahoraIso(): String =
        LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
}
