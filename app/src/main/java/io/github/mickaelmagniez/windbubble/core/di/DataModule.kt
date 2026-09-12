package io.github.mickaelmagniez.windbubble.core.di

import io.github.mickaelmagniez.windbubble.data.location.SystemLocationRepository
import io.github.mickaelmagniez.windbubble.data.sensor.SensorCompassRepository
import io.github.mickaelmagniez.windbubble.data.settings.SettingsDataStoreRepository
import io.github.mickaelmagniez.windbubble.data.wind.OpenMeteoWindRepository
import io.github.mickaelmagniez.windbubble.domain.repository.CompassRepository
import io.github.mickaelmagniez.windbubble.domain.repository.LocationRepository
import io.github.mickaelmagniez.windbubble.domain.repository.SettingsRepository
import io.github.mickaelmagniez.windbubble.domain.repository.WindRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    abstract fun bindWindRepository(impl: OpenMeteoWindRepository): WindRepository

    @Binds
    abstract fun bindLocationRepository(impl: SystemLocationRepository): LocationRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsDataStoreRepository): SettingsRepository

    @Binds
    abstract fun bindCompassRepository(impl: SensorCompassRepository): CompassRepository

}
