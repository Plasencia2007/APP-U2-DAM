package pe.upeu.ecorecicla

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pe.upeu.ecorecicla.di.modulosEcoRutas

class EcoRutasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@EcoRutasApplication)
            modules(modulosEcoRutas(platformModule))
        }
    }
}
