package io.github.mickaelmagniez.windbubble.session

import io.github.mickaelmagniez.windbubble.core.di.ApplicationScope
import io.github.mickaelmagniez.windbubble.domain.model.WindSessionState
import io.github.mickaelmagniez.windbubble.domain.usecase.ObserveRelativeWindUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Shares one wind session between the dashboard and the floating bubble: the GPS and the network
 * are only active while at least one of them observes [state].
 */
@Singleton
class WindSessionManager @Inject constructor(
    observeRelativeWind: ObserveRelativeWindUseCase,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {
    /** Bumped when permissions change, so the pipeline restarts from scratch. */
    private val restartSignal = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<WindSessionState> = restartSignal
        .flatMapLatest { observeRelativeWind() }
        .stateIn(
            scope = applicationScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = WindSessionState(),
        )

    fun restart() = restartSignal.update { it + 1 }

    private companion object {
        val STOP_TIMEOUT_MILLIS = 5.seconds.inWholeMilliseconds
    }
}
