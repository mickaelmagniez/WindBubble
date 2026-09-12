package io.github.mickaelmagniez.windbubble.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.mickaelmagniez.windbubble.R
import androidx.annotation.StringRes
import io.github.mickaelmagniez.windbubble.domain.model.HeadingSource
import io.github.mickaelmagniez.windbubble.domain.model.WindCategory

@Composable
fun WindCategory.label(): String = stringResource(
    when (this) {
        WindCategory.HEADWIND -> R.string.wind_headwind
        WindCategory.HEAD_CROSSWIND -> R.string.wind_head_crosswind
        WindCategory.CROSSWIND -> R.string.wind_crosswind
        WindCategory.TAIL_CROSSWIND -> R.string.wind_tail_crosswind
        WindCategory.TAILWIND -> R.string.wind_tailwind
    },
)

/**
 * Explains where the heading comes from when it is not the direction you are actually travelling;
 * `null` while the GPS course is live, which needs no explanation.
 */
@StringRes
fun HeadingSource.hintOrNull(): Int? = when (this) {
    HeadingSource.MOVEMENT -> null
    HeadingSource.COMPASS -> R.string.home_heading_compass
    HeadingSource.LAST_KNOWN -> R.string.home_heading_stale
}
