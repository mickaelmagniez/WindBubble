package io.github.mickaelmagniez.windbubble.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import io.github.mickaelmagniez.windbubble.core.util.angleDelta
import kotlin.math.abs

/**
 * Continuous colour scale for the relative wind: red straight in your face, amber across, green
 * pushing you along. The hue is interpolated so neighbouring angles never jump between buckets.
 */
@Composable
@ReadOnlyComposable
fun windColor(relativeAngleDegrees: Float): Color {
    val dark = !MaterialTheme.colorScheme.isLight()
    // 0° (headwind) -> hue 0 (red), 180° (tailwind) -> hue 120 (green).
    val foldedAngle = abs(angleDelta(from = 0f, to = relativeAngleDegrees))
    val hue = HUE_TAILWIND * (foldedAngle / 180f)
    // Pure yellow reads badly on light surfaces, so mid hues are darkened there.
    val yellowness = 1f - (abs(hue - HUE_MID) / HUE_MID)
    return if (dark) {
        Color.hsv(hue = hue, saturation = 0.62f - 0.12f * yellowness, value = 0.98f)
    } else {
        Color.hsv(hue = hue, saturation = 0.95f, value = 0.78f - 0.16f * yellowness)
    }
}

/** Neutral colour used while no relative wind is known yet. */
@Composable
@ReadOnlyComposable
fun pendingWindColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)

@Composable
@ReadOnlyComposable
internal fun androidx.compose.material3.ColorScheme.isLight(): Boolean =
    background.luminance() > 0.5f

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

private const val HUE_TAILWIND = 120f
private const val HUE_MID = 60f
