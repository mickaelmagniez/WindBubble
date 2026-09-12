package io.github.mickaelmagniez.windbubble.ui.components

import androidx.annotation.StringRes
import io.github.mickaelmagniez.windbubble.R
import io.github.mickaelmagniez.windbubble.domain.model.WindSessionState

/** Message explaining what the session is still waiting for, shared by the screen and the bubble. */
@StringRes
fun WindSessionState.statusMessage(): Int = when (status) {
    WindSessionState.Status.IDLE -> R.string.home_idle
    WindSessionState.Status.WAITING_FOR_LOCATION -> R.string.home_waiting_location
    WindSessionState.Status.WAITING_FOR_WIND -> R.string.home_waiting_wind
    WindSessionState.Status.WAITING_FOR_HEADING -> R.string.home_waiting_heading
    WindSessionState.Status.LIVE -> R.string.home_idle
}
