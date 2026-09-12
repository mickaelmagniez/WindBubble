package io.github.mickaelmagniez.windbubble.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mickaelmagniez.windbubble.R
import io.github.mickaelmagniez.windbubble.core.util.toCardinalPoint
import io.github.mickaelmagniez.windbubble.domain.model.RelativeWind
import io.github.mickaelmagniez.windbubble.domain.model.SpeedUnit
import io.github.mickaelmagniez.windbubble.domain.model.WindSessionState
import io.github.mickaelmagniez.windbubble.ui.components.WindDial
import io.github.mickaelmagniez.windbubble.ui.components.hintOrNull
import io.github.mickaelmagniez.windbubble.ui.components.statusMessage
import io.github.mickaelmagniez.windbubble.ui.components.label
import io.github.mickaelmagniez.windbubble.ui.theme.windColor

private val BUBBLE_BASE_SIZE = 88.dp

/**
 * The floating bubble itself: a compact dial that can be dragged anywhere, and expands into a
 * short summary when tapped.
 */
@Composable
fun WindBubbleContent(
    state: WindSessionState,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = state.preferences
    Column(
        modifier = modifier.alpha(preferences.bubbleOpacity),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(BUBBLE_BASE_SIZE * preferences.bubbleScale)
                // The tap detector must sit *above* the drag one: pointer events reach the
                // innermost modifier first, so the drag gets to claim the gesture and the tap is
                // cancelled as soon as a drag consumes a move.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onToggleExpanded() })
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                    ) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                },
        ) {
            BubbleFace(state = state, unit = preferences.speedUnit)
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            BubbleDetails(state = state, onClose = onClose)
        }
    }
}

@Composable
private fun BubbleFace(state: WindSessionState, unit: SpeedUnit) {
    val relativeWind = state.relativeWind
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        WindDial(
            relativeAngleDegrees = relativeWind?.relativeAngleDegrees,
            modifier = Modifier.fillMaxSize().padding(4.dp),
        ) {
            // The wind speed is worth showing even before the GPS gives us a heading.
            val windSpeed = relativeWind?.windSpeedMetersPerSecond
                ?: state.wind?.speedMetersPerSecond
            if (windSpeed == null) {
                Text(
                    text = stringResource(R.string.overlay_waiting),
                    style = MaterialTheme.typography.titleMedium,
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = unit.displayValue(windSpeed).toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = relativeWind?.relativeAngleDegrees?.let { windColor(it) }
                            ?: MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = unit.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleDetails(state: WindSessionState, onClose: () -> Unit) {
    val relativeWind = state.relativeWind
    val unit = state.preferences.speedUnit
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        modifier = Modifier.width(200.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (relativeWind == null) {
                    Text(
                        text = stringResource(state.statusMessage()),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start,
                    )
                } else {
                    Text(
                        text = relativeWind.category.label(),
                        style = MaterialTheme.typography.titleSmall,
                        color = windColor(relativeWind.relativeAngleDegrees),
                    )
                    Text(
                        text = relativeWind.summaryLine(unit, state.preferences.showGusts),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    relativeWind.headingSource.hintOrNull()?.let { hint ->
                        Text(
                            text = stringResource(hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.overlay_close),
                )
            }
        }
    }
}

/** One-line recap such as "18 km/h · 27 max · from WNW". */
@Composable
private fun RelativeWind.summaryLine(unit: SpeedUnit, showGusts: Boolean): String {
    val gust = gustMetersPerSecond
    val parts = buildList {
        add(unit.format(windSpeedMetersPerSecond))
        if (showGusts && gust != null && gust > windSpeedMetersPerSecond) {
            add(stringResource(R.string.bubble_summary_gust, unit.displayValue(gust)))
        }
        add(stringResource(R.string.bubble_summary_from, windDirectionFromDegrees.toCardinalPoint()))
    }
    return parts.joinToString(separator = " · ")
}
