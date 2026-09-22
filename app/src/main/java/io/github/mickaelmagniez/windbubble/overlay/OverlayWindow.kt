package io.github.mickaelmagniez.windbubble.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.getSystemService
import io.github.mickaelmagniez.windbubble.domain.model.UserPreferences
import io.github.mickaelmagniez.windbubble.domain.model.WindSessionState
import io.github.mickaelmagniez.windbubble.ui.theme.WindBubbleTheme
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the `WindowManager` view holding the bubble: adding it, moving it while the user drags it
 * and tearing it down.
 */
class OverlayWindow(
    private val context: Context,
    private val state: StateFlow<WindSessionState>,
    private val onPositionChanged: (x: Int, y: Int) -> Unit,
    private val onCloseRequested: () -> Unit,
) {
    private val windowManager = requireNotNull(context.getSystemService<WindowManager>())
    private val owner = OverlayViewOwner()
    private var composeView: ComposeView? = null

    // FLAG_SHOW_WHEN_LOCKED is deprecated for activities only; on a WindowManager window it is
    // still the only way to survive the keyguard. Without it the system hides the bubble as soon as
    // the lock screen shows, which happens whenever a navigation app wakes the screen before a turn.
    @Suppress("DEPRECATION")
    private val windowParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    fun show(preferences: UserPreferences) {
        if (composeView != null) return
        applyStartPosition(preferences)

        val view = ComposeView(context).apply {
            setContent {
                val sessionState by state.collectAsState()
                var expanded by remember { mutableStateOf(false) }
                WindBubbleTheme {
                    WindBubbleContent(
                        state = sessionState,
                        expanded = expanded,
                        onToggleExpanded = { expanded = !expanded },
                        onClose = onCloseRequested,
                        onDrag = ::moveBy,
                        onDragEnd = { onPositionChanged(windowParams.x, windowParams.y) },
                    )
                }
            }
        }
        // Expanding the bubble grows the window; without this the card would overflow the screen
        // edge and drag the bubble out of view with it.
        view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val sizeChanged = (right - left) != (oldRight - oldLeft) ||
                (bottom - top) != (oldBottom - oldTop)
            if (sizeChanged) view.post { keepInsideScreen() }
        }
        owner.attachTo(view)
        composeView = view
        windowManager.addView(view, windowParams)
    }

    fun dismiss() {
        composeView?.let { view ->
            windowManager.removeViewImmediate(view)
            view.disposeComposition()
        }
        composeView = null
        owner.detach()
    }

    private fun keepInsideScreen() {
        val view = composeView ?: return
        val previousX = windowParams.x
        val previousY = windowParams.y
        coerceIntoScreen(view.width, view.height)
        if (previousX != windowParams.x || previousY != windowParams.y) {
            windowManager.updateViewLayout(view, windowParams)
        }
    }

    private fun moveBy(dragAmount: Offset) {
        val view = composeView ?: return
        windowParams.x += dragAmount.x.roundToInt()
        windowParams.y += dragAmount.y.roundToInt()
        coerceIntoScreen(view.width, view.height)
        windowManager.updateViewLayout(view, windowParams)
    }

    /** Re-applies a position changed from the settings screen, e.g. after a reset. */
    fun onSavedPositionChanged(preferences: UserPreferences) {
        val view = composeView ?: return
        val (x, y) = preferences.resolvePosition()
        if (x == windowParams.x && y == windowParams.y) return
        windowParams.x = x
        windowParams.y = y
        coerceIntoScreen(view.width, view.height)
        windowManager.updateViewLayout(view, windowParams)
    }

    private fun applyStartPosition(preferences: UserPreferences) {
        val (x, y) = preferences.resolvePosition()
        windowParams.x = x
        windowParams.y = y
    }

    /** Falls back to a spot on the upper right of the screen until the user drags the bubble. */
    private fun UserPreferences.resolvePosition(): Pair<Int, Int> {
        val metrics = context.resources.displayMetrics
        val hasSavedPosition = bubblePositionX != UserPreferences.DEFAULT_BUBBLE_POSITION
        return if (hasSavedPosition) {
            bubblePositionX to bubblePositionY
        } else {
            metrics.defaultStartX() to metrics.defaultStartY()
        }
    }

    private fun coerceIntoScreen(viewWidth: Int, viewHeight: Int) {
        val metrics = context.resources.displayMetrics
        val maxX = (metrics.widthPixels - viewWidth).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - viewHeight).coerceAtLeast(0)
        windowParams.x = windowParams.x.coerceIn(0, maxX)
        windowParams.y = windowParams.y.coerceIn(0, maxY)
    }

    private fun DisplayMetrics.defaultStartX(): Int = (widthPixels * 0.72f).roundToInt()

    private fun DisplayMetrics.defaultStartY(): Int = (heightPixels * 0.25f).roundToInt()
}