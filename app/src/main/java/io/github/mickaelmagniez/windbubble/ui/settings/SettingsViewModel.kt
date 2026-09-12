package io.github.mickaelmagniez.windbubble.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mickaelmagniez.windbubble.domain.model.SpeedUnit
import io.github.mickaelmagniez.windbubble.domain.model.UserPreferences
import io.github.mickaelmagniez.windbubble.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.preferences
        .map(::SettingsUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(UserPreferences()),
        )

    fun onSpeedUnitSelected(unit: SpeedUnit) = update { setSpeedUnit(unit) }

    fun onRefreshIntervalChanged(minutes: Int) = update { setRefreshIntervalMinutes(minutes) }

    fun onBubbleOpacityChanged(opacity: Float) = update { setBubbleOpacity(opacity) }

    fun onBubbleScaleChanged(scale: Float) = update { setBubbleScale(scale) }

    fun onShowGustsChanged(show: Boolean) = update { setShowGusts(show) }

    fun onResetBubblePosition() = update { resetBubblePosition() }

    private fun update(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { settingsRepository.block() }
    }

    private companion object {
        val STOP_TIMEOUT_MILLIS = 5.seconds.inWholeMilliseconds
    }
}

data class SettingsUiState(val preferences: UserPreferences)
