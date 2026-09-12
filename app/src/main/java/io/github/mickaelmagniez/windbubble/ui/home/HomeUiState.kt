package io.github.mickaelmagniez.windbubble.ui.home

import io.github.mickaelmagniez.windbubble.domain.model.WindSessionState

/** Everything [HomeScreen] renders, produced by [HomeViewModel] only. */
data class HomeUiState(
    val session: WindSessionState = WindSessionState(),
    val isBubbleRunning: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
) {
    val canStartBubble: Boolean get() = hasLocationPermission && hasOverlayPermission
}

/** One-shot effects the screen must react to, kept separate from the state. */
sealed interface HomeEvent {
    data object RequestLocationPermission : HomeEvent
    data object RequestNotificationPermission : HomeEvent
    data object RequestOverlayPermission : HomeEvent
}
