package io.github.mickaelmagniez.windbubble.overlay

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Starts and stops the bubble, and lets the UI observe whether it is currently displayed. */
@Singleton
class OverlayController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun start() {
        if (!context.canDrawOverlay()) return
        ContextCompat.startForegroundService(context, Intent(context, WindOverlayService::class.java))
    }

    fun stop() {
        context.startService(
            Intent(context, WindOverlayService::class.java).setAction(WindOverlayService.ACTION_STOP),
        )
    }

    internal fun onServiceStateChanged(running: Boolean) {
        _isRunning.value = running
    }
}
