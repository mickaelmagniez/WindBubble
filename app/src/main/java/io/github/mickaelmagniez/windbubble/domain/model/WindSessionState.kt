package io.github.mickaelmagniez.windbubble.domain.model

/** Single source of truth describing what the app currently knows about the wind. */
data class WindSessionState(
    val status: Status = Status.IDLE,
    val relativeWind: RelativeWind? = null,
    val movement: Movement? = null,
    val wind: WindObservation? = null,
    val error: WindSessionError? = null,
    val preferences: UserPreferences = UserPreferences(),
) {
    enum class Status {
        /** Nothing is subscribed yet. */
        IDLE,

        /** Waiting for the first GPS fix. */
        WAITING_FOR_LOCATION,

        /** We have a position but no wind reading yet. */
        WAITING_FOR_WIND,

        /** The wind is known, but the user is too slow for the GPS to give a heading. */
        WAITING_FOR_HEADING,

        /** Everything is available; [relativeWind] is non-null. */
        LIVE,
    }

    val isLive: Boolean get() = status == Status.LIVE && relativeWind != null
}

/** Recoverable problems worth surfacing to the user. */
sealed interface WindSessionError {
    data object MissingLocationPermission : WindSessionError
    data object LocationUnavailable : WindSessionError
    data class WindFetchFailed(val reason: String) : WindSessionError
}
