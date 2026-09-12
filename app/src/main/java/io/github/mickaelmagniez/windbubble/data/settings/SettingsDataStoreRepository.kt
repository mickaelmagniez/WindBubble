package io.github.mickaelmagniez.windbubble.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.mickaelmagniez.windbubble.domain.model.SpeedUnit
import io.github.mickaelmagniez.windbubble.domain.model.UserPreferences
import io.github.mickaelmagniez.windbubble.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wind_bubble_settings")

@Singleton
class SettingsDataStoreRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {

    private val dataStore = context.dataStore

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map(::toUserPreferences)

    override suspend fun setSpeedUnit(unit: SpeedUnit) = edit { it[Keys.SPEED_UNIT] = unit.name }

    override suspend fun setRefreshIntervalMinutes(minutes: Int) = edit {
        it[Keys.REFRESH_INTERVAL] = minutes.coerceIn(
            UserPreferences.MIN_REFRESH_INTERVAL_MINUTES,
            UserPreferences.MAX_REFRESH_INTERVAL_MINUTES,
        )
    }

    override suspend fun setBubbleOpacity(opacity: Float) = edit {
        it[Keys.BUBBLE_OPACITY] = opacity.coerceIn(MIN_OPACITY, 1f)
    }

    override suspend fun setBubbleScale(scale: Float) = edit {
        it[Keys.BUBBLE_SCALE] = scale.coerceIn(
            UserPreferences.MIN_BUBBLE_SCALE,
            UserPreferences.MAX_BUBBLE_SCALE,
        )
    }

    override suspend fun setShowGusts(show: Boolean) = edit { it[Keys.SHOW_GUSTS] = show }

    override suspend fun setBubblePosition(x: Int, y: Int) = edit {
        it[Keys.BUBBLE_X] = x
        it[Keys.BUBBLE_Y] = y
    }

    override suspend fun resetBubblePosition() = edit {
        it.remove(Keys.BUBBLE_X)
        it.remove(Keys.BUBBLE_Y)
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private fun toUserPreferences(preferences: Preferences): UserPreferences {
        val defaults = UserPreferences()
        return UserPreferences(
            speedUnit = preferences[Keys.SPEED_UNIT]?.let(::speedUnitOrNull) ?: defaults.speedUnit,
            refreshIntervalMinutes = preferences[Keys.REFRESH_INTERVAL] ?: defaults.refreshIntervalMinutes,
            bubbleOpacity = preferences[Keys.BUBBLE_OPACITY] ?: defaults.bubbleOpacity,
            bubbleScale = preferences[Keys.BUBBLE_SCALE] ?: defaults.bubbleScale,
            showGusts = preferences[Keys.SHOW_GUSTS] ?: defaults.showGusts,
            bubblePositionX = preferences[Keys.BUBBLE_X] ?: defaults.bubblePositionX,
            bubblePositionY = preferences[Keys.BUBBLE_Y] ?: defaults.bubblePositionY,
        )
    }

    private fun speedUnitOrNull(name: String): SpeedUnit? =
        SpeedUnit.entries.firstOrNull { it.name == name }

    private object Keys {
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val REFRESH_INTERVAL = intPreferencesKey("refresh_interval_minutes")
        val BUBBLE_OPACITY = floatPreferencesKey("bubble_opacity")
        val BUBBLE_SCALE = floatPreferencesKey("bubble_scale")
        val SHOW_GUSTS = booleanPreferencesKey("show_gusts")
        val BUBBLE_X = intPreferencesKey("bubble_position_x")
        val BUBBLE_Y = intPreferencesKey("bubble_position_y")
    }

    private companion object {
        const val MIN_OPACITY = 0.3f
    }
}
