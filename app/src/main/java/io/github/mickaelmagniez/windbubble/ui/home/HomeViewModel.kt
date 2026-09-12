package io.github.mickaelmagniez.windbubble.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.mickaelmagniez.windbubble.domain.repository.LocationRepository
import io.github.mickaelmagniez.windbubble.overlay.OverlayController
import io.github.mickaelmagniez.windbubble.overlay.canDrawOverlay
import io.github.mickaelmagniez.windbubble.session.WindSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionManager: WindSessionManager,
    private val overlayController: OverlayController,
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val permissions = MutableStateFlow(readPermissions())

    /** Permissions already offered in this session, so the automatic flow never loops. */
    private val alreadyRequested = mutableSetOf<HomeEvent>()
    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow: Flow<HomeEvent> = events.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        sessionManager.state,
        overlayController.isRunning,
        permissions,
    ) { session, isBubbleRunning, permissionState ->
        HomeUiState(
            session = session,
            isBubbleRunning = isBubbleRunning,
            hasLocationPermission = permissionState.location,
            hasOverlayPermission = permissionState.overlay,
            hasNotificationPermission = permissionState.notifications,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState(),
    )

    /**
     * Called whenever the screen resumes, since permissions can change outside the app, and after
     * every permission dialog closes.
     */
    fun refreshPermissions() {
        val updated = readPermissions()
        val locationJustGranted = updated.location && !permissions.value.location
        permissions.value = updated
        if (locationJustGranted) sessionManager.restart()
        requestNextMissingPermission()
    }

    /**
     * Walks through the missing permissions on its own so the user does not have to hunt for the
     * buttons. Each one is only ever offered once per session, so a refusal is never nagged at.
     */
    private fun requestNextMissingPermission() {
        val state = permissions.value
        val next = when {
            !state.location -> HomeEvent.RequestLocationPermission
            !state.notifications -> HomeEvent.RequestNotificationPermission
            !state.overlay -> HomeEvent.RequestOverlayPermission
            else -> null
        }
        if (next == null || !alreadyRequested.add(next)) return
        viewModelScope.launch { events.send(next) }
    }

    fun onBubbleToggled(enabled: Boolean) {
        when {
            !enabled -> overlayController.stop()
            !permissions.value.overlay -> viewModelScope.launch {
                alreadyRequested += HomeEvent.RequestOverlayPermission
                events.send(HomeEvent.RequestOverlayPermission)
            }

            else -> overlayController.start()
        }
    }

    private fun readPermissions() = PermissionState(
        location = locationRepository.hasLocationPermission(),
        overlay = context.canDrawOverlay(),
        notifications = hasNotificationPermission(),
    )

    private fun hasNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            true
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }

    private data class PermissionState(
        val location: Boolean,
        val overlay: Boolean,
        val notifications: Boolean,
    )

    private companion object {
        val STOP_TIMEOUT_MILLIS = 5.seconds.inWholeMilliseconds
    }
}
