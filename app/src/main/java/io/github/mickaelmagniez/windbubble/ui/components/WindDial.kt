package io.github.mickaelmagniez.windbubble.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.mickaelmagniez.windbubble.core.util.angleDelta
import io.github.mickaelmagniez.windbubble.ui.theme.pendingWindColor
import io.github.mickaelmagniez.windbubble.ui.theme.windColor

/**
 * The core visual of the app: a compass locked on your direction of travel.
 *
 * The top of the dial is where you are heading — marked by the small grey tick outside the ring —
 * and the coloured marker sits on the ring at the angle the wind comes from, red when it blows in
 * your face, green when it pushes you along.
 */
@Composable
fun WindDial(
    relativeAngleDegrees: Float?,
    modifier: Modifier = Modifier,
    headingColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    animated: Boolean = true,
    centerContent: @Composable BoxScope.() -> Unit = {},
) {
    val targetAngle = rememberContinuousAngle(relativeAngleDegrees ?: 0f)
    val angle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = if (animated) ANGLE_ANIMATION_MILLIS else 0),
        label = "windAngle",
    )
    val markerColor by animateColorAsState(
        targetValue = relativeAngleDegrees?.let { windColor(it) } ?: pendingWindColor(),
        animationSpec = tween(durationMillis = COLOR_ANIMATION_MILLIS),
        label = "windColor",
    )
    val ringBrush = Brush.sweepGradient(
        0.00f to windColor(90f),
        0.25f to windColor(180f),
        0.50f to windColor(270f),
        0.75f to windColor(0f),
        1.00f to windColor(90f),
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawScale(radius, ringBrush)
            drawHeadingTick(radius, headingColor)
            // Without a heading or a reading there is nothing to point at yet.
            if (relativeAngleDegrees != null) {
                rotate(degrees = angle) {
                    drawWindMarker(radius, markerColor)
                }
            }
        }
        centerContent()
    }
}

/**
 * Unwraps the compass angle into a continuous value so the marker animates through the shortest
 * path instead of spinning all the way around when it crosses north.
 */
@Composable
private fun rememberContinuousAngle(angleDegrees: Float): Float {
    val unwrapper = remember { AngleUnwrapper(angleDegrees) }
    return remember(angleDegrees) { unwrapper.unwrap(angleDegrees) }
}

/** Plain holder, deliberately not a snapshot state: it is only read during composition. */
private class AngleUnwrapper(initialAngle: Float) {
    private var previous = initialAngle
    private var continuous = initialAngle

    fun unwrap(angleDegrees: Float): Float {
        continuous += angleDelta(previous, angleDegrees)
        previous = angleDegrees
        return continuous
    }
}

/** The ring itself doubles as the legend: red at the top, green at the bottom. */
private fun DrawScope.drawScale(radius: Float, brush: Brush) {
    drawCircle(
        brush = brush,
        radius = radius * RING_RADIUS,
        alpha = RING_ALPHA,
        style = Stroke(width = radius * RING_WIDTH),
    )
}

/** Small grey tick just outside the ring: that is you, moving forward. */
private fun DrawScope.drawHeadingTick(radius: Float, color: Color) {
    val halfWidth = radius * 0.075f
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 0.99f)
        lineTo(center.x + halfWidth, center.y - radius * 0.87f)
        lineTo(center.x - halfWidth, center.y - radius * 0.87f)
        close()
    }
    drawPath(path, color)
}

/**
 * Chevron riding on the ring, pointing inwards: the wind comes from there and blows towards you.
 * Drawn straight up so a rotation of `relativeAngle` puts it exactly where the wind comes from.
 */
private fun DrawScope.drawWindMarker(radius: Float, color: Color) {
    val halfWidth = radius * 0.19f
    val path = Path().apply {
        moveTo(center.x, center.y - radius * 0.56f)
        lineTo(center.x + halfWidth, center.y - radius * 0.82f)
        lineTo(center.x, center.y - radius * 0.73f)
        lineTo(center.x - halfWidth, center.y - radius * 0.82f)
        close()
    }
    drawPath(path, color)
}

private const val RING_RADIUS = 0.72f
private const val RING_WIDTH = 0.13f
private const val RING_ALPHA = 0.38f
private const val ANGLE_ANIMATION_MILLIS = 600
private const val COLOR_ANIMATION_MILLIS = 400
