package io.github.mickaelmagniez.windbubble.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mickaelmagniez.windbubble.R
import io.github.mickaelmagniez.windbubble.domain.model.SpeedUnit
import io.github.mickaelmagniez.windbubble.domain.model.UserPreferences
import kotlin.math.roundToInt

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        preferences = uiState.preferences,
        onNavigateBack = onNavigateBack,
        onSpeedUnitSelected = viewModel::onSpeedUnitSelected,
        onRefreshIntervalChanged = viewModel::onRefreshIntervalChanged,
        onBubbleOpacityChanged = viewModel::onBubbleOpacityChanged,
        onBubbleScaleChanged = viewModel::onBubbleScaleChanged,
        onShowGustsChanged = viewModel::onShowGustsChanged,
        onResetBubblePosition = viewModel::onResetBubblePosition,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    preferences: UserPreferences,
    onNavigateBack: () -> Unit,
    onSpeedUnitSelected: (SpeedUnit) -> Unit,
    onRefreshIntervalChanged: (Int) -> Unit,
    onBubbleOpacityChanged: (Float) -> Unit,
    onBubbleScaleChanged: (Float) -> Unit,
    onShowGustsChanged: (Boolean) -> Unit,
    onResetBubblePosition: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCard(title = stringResource(R.string.settings_units_title)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SpeedUnit.entries.forEachIndexed { index, unit ->
                        SegmentedButton(
                            selected = preferences.speedUnit == unit,
                            onClick = { onSpeedUnitSelected(unit) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = SpeedUnit.entries.size,
                            ),
                        ) {
                            Text(unit.label)
                        }
                    }
                }
            }

            SettingsCard(title = stringResource(R.string.settings_refresh_title)) {
                Text(
                    text = stringResource(
                        R.string.settings_refresh_value,
                        preferences.refreshIntervalMinutes,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = preferences.refreshIntervalMinutes.toFloat(),
                    onValueChange = { onRefreshIntervalChanged(it.roundToInt()) },
                    valueRange = UserPreferences.MIN_REFRESH_INTERVAL_MINUTES.toFloat()..
                        UserPreferences.MAX_REFRESH_INTERVAL_MINUTES.toFloat(),
                    steps = UserPreferences.MAX_REFRESH_INTERVAL_MINUTES -
                        UserPreferences.MIN_REFRESH_INTERVAL_MINUTES - 1,
                )
                Text(
                    text = stringResource(R.string.settings_refresh_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SettingsCard(title = stringResource(R.string.settings_bubble_title)) {
                LabeledSlider(
                    label = stringResource(R.string.settings_bubble_opacity),
                    value = preferences.bubbleOpacity,
                    valueRange = 0.3f..1f,
                    onValueChange = onBubbleOpacityChanged,
                    valueLabel = "${(preferences.bubbleOpacity * 100).roundToInt()} %",
                )
                LabeledSlider(
                    label = stringResource(R.string.settings_bubble_scale),
                    value = preferences.bubbleScale,
                    valueRange = UserPreferences.MIN_BUBBLE_SCALE..UserPreferences.MAX_BUBBLE_SCALE,
                    onValueChange = onBubbleScaleChanged,
                    valueLabel = "${(preferences.bubbleScale * 100).roundToInt()} %",
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(R.string.settings_bubble_gusts))
                    Switch(checked = preferences.showGusts, onCheckedChange = onShowGustsChanged)
                }
                TextButton(onClick = onResetBubblePosition) {
                    Text(stringResource(R.string.settings_bubble_reset_position))
                }
            }

            SettingsCard(title = stringResource(R.string.settings_about_title)) {
                Text(
                    text = stringResource(R.string.settings_about_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.home_data_source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueLabel: String,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange)
    }
}
