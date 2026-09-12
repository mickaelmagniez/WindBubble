package io.github.mickaelmagniez.windbubble.domain.repository

import io.github.mickaelmagniez.windbubble.domain.model.SpeedUnit
import io.github.mickaelmagniez.windbubble.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setSpeedUnit(unit: SpeedUnit)
    suspend fun setRefreshIntervalMinutes(minutes: Int)
    suspend fun setBubbleOpacity(opacity: Float)
    suspend fun setBubbleScale(scale: Float)
    suspend fun setShowGusts(show: Boolean)
    suspend fun setBubblePosition(x: Int, y: Int)
    suspend fun resetBubblePosition()
}
