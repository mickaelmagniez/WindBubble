package io.github.mickaelmagniez.windbubble.overlay

import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.mickaelmagniez.windbubble.domain.repository.SettingsRepository
import io.github.mickaelmagniez.windbubble.session.WindSessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service keeping the GPS stream and the bubble alive while the user navigates in
 * another app. It owns no state of its own: everything comes from [WindSessionManager].
 */
@AndroidEntryPoint
class WindOverlayService : LifecycleService() {

    @Inject lateinit var sessionManager: WindSessionManager
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var overlayController: OverlayController

    private var overlayWindow: OverlayWindow? = null

    override fun onCreate() {
        super.onCreate()
        OverlayNotifications.ensureChannel(this)
        if (!startAsForegroundService()) {
            stopSelf()
            return
        }
        overlayController.onServiceStateChanged(running = true)
        keepSessionWarm()
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        overlayWindow?.dismiss()
        overlayWindow = null
        overlayController.onServiceStateChanged(running = false)
        super.onDestroy()
    }

    /**
     * Collecting the shared session here guarantees the GPS and the wind polling keep running even
     * if the bubble composition is paused by the system.
     */
    private fun keepSessionWarm() {
        lifecycleScope.launch { sessionManager.state.collect { } }
    }

    private fun showBubble() {
        lifecycleScope.launch {
            val preferences = settingsRepository.preferences.first()
            if (!canDrawOverlay()) {
                stopSelf()
                return@launch
            }
            overlayWindow = OverlayWindow(
                context = this@WindOverlayService,
                state = sessionManager.state,
                onPositionChanged = { x, y ->
                    lifecycleScope.launch { settingsRepository.setBubblePosition(x, y) }
                },
                onCloseRequested = { stopSelf() },
            ).also { it.show(preferences) }

            observeSavedPosition()
        }
    }

    /** Keeps the bubble in sync when the position is changed from the settings screen. */
    private fun observeSavedPosition() {
        lifecycleScope.launch {
            settingsRepository.preferences
                .distinctUntilChangedBy { it.bubblePositionX to it.bubblePositionY }
                .collect { overlayWindow?.onSavedPositionChanged(it) }
        }
    }

    /**
     * The service must run in the foreground with the `location` type, otherwise Android stops
     * feeding it GPS updates as soon as the user switches to the navigation app.
     */
    private fun startAsForegroundService(): Boolean = runCatching {
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            OverlayNotifications.NOTIFICATION_ID,
            OverlayNotifications.build(this),
            serviceType,
        )
    }.onFailure { Log.w(TAG, "Unable to start the overlay service in the foreground", it) }
        .isSuccess

    companion object {
        const val ACTION_STOP = "io.github.mickaelmagniez.windbubble.action.STOP_OVERLAY"
        private const val TAG = "WindOverlayService"
    }
}
