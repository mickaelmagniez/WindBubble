package io.github.mickaelmagniez.windbubble.ui.home

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mickaelmagniez.windbubble.R
import io.github.mickaelmagniez.windbubble.core.util.toCardinalPoint
import io.github.mickaelmagniez.windbubble.domain.model.RelativeWind
import io.github.mickaelmagniez.windbubble.domain.model.SpeedUnit
import io.github.mickaelmagniez.windbubble.domain.model.WindObservation
import io.github.mickaelmagniez.windbubble.domain.model.WindSessionError
import io.github.mickaelmagniez.windbubble.domain.model.WindSessionState
import io.github.mickaelmagniez.windbubble.overlay.overlaySettingsIntent
import io.github.mickaelmagniez.windbubble.ui.components.WindDial
import io.github.mickaelmagniez.windbubble.ui.components.hintOrNull
import io.github.mickaelmagniez.windbubble.ui.components.statusMessage
import io.github.mickaelmagniez.windbubble.ui.components.label
import io.github.mickaelmagniez.windbubble.ui.theme.windColor
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeRoute(
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermissions()
        onPauseOrDispose { }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshPermissions() }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshPermissions() }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { viewModel.refreshPermissions() }

    val overlayHint = stringResource(R.string.permission_overlay_hint)
    LaunchedEffectOnEvents(viewModel) { event ->
        when (event) {
            HomeEvent.RequestLocationPermission -> locationLauncher.launch(LOCATION_PERMISSIONS)
            HomeEvent.RequestNotificationPermission ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

            HomeEvent.RequestOverlayPermission -> {
                // Android no longer deep-links to this app's own row, so say what to look for.
                Toast.makeText(context, overlayHint, Toast.LENGTH_LONG).show()
                overlayLauncher.launch(context.overlaySettingsIntent())
            }
        }
    }

    HomeScreen(
        uiState = uiState,
        onOpenSettings = onOpenSettings,
        onBubbleToggled = viewModel::onBubbleToggled,
        onRequestLocation = { locationLauncher.launch(LOCATION_PERMISSIONS) },
        onRequestOverlay = {
            Toast.makeText(context, overlayHint, Toast.LENGTH_LONG).show()
            overlayLauncher.launch(context.overlaySettingsIntent())
        },
        onRequestNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
    )
}

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

@Composable
private fun LaunchedEffectOnEvents(
    viewModel: HomeViewModel,
    onEvent: suspend (HomeEvent) -> Unit,
) {
    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest(onEvent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onOpenSettings: () -> Unit,
    onBubbleToggled: (Boolean) -> Unit,
    onRequestLocation: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.home_settings))
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
            WindSummaryCard(uiState.session)
            BubbleCard(
                isRunning = uiState.isBubbleRunning,
                canStart = uiState.canStartBubble,
                onBubbleToggled = onBubbleToggled,
            )
            PermissionsSection(
                uiState = uiState,
                onRequestLocation = onRequestLocation,
                onRequestOverlay = onRequestOverlay,
                onRequestNotifications = onRequestNotifications,
            )
            Text(
                text = stringResource(R.string.home_data_source),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun WindSummaryCard(session: WindSessionState) {
    val relativeWind = session.relativeWind
    val unit = session.preferences.speedUnit
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                WindDial(
                    relativeAngleDegrees = relativeWind?.relativeAngleDegrees,
                    modifier = Modifier.size(220.dp),
                ) {
                    // Once the wind is known its speed is shown, heading or not.
                    val windSpeed = session.wind?.speedMetersPerSecond
                    if (windSpeed != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = unit.displayValue(windSpeed).toString(),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = unit.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (relativeWind != null) {
                Text(
                    text = relativeWind.category.label(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = windColor(relativeWind.relativeAngleDegrees),
                )
                relativeWind.headingSource.hintOrNull()?.let { hint ->
                    Text(
                        text = stringResource(hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                HorizontalDivider()
                WindDetails(relativeWind = relativeWind, session = session, unit = unit)
            } else {
                Text(
                    text = stringResource(session.statusMessage()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                session.wind?.let { wind ->
                    HorizontalDivider()
                    AbsoluteWindDetails(wind = wind, session = session, unit = unit)
                }
            }

            session.error?.let { error ->
                Text(
                    text = error.message(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Shown while the GPS has not given us a heading yet: the plain, non-relative wind. */
@Composable
private fun AbsoluteWindDetails(
    wind: WindObservation,
    session: WindSessionState,
    unit: SpeedUnit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        DetailRow(
            label = stringResource(R.string.home_detail_wind),
            value = "${unit.format(wind.speedMetersPerSecond)} " +
                "(${wind.directionFromDegrees.toCardinalPoint()})",
        )
        wind.gustMetersPerSecond?.takeIf { session.preferences.showGusts }?.let { gust ->
            DetailRow(
                label = stringResource(R.string.home_detail_gusts),
                value = unit.format(gust),
            )
        }
        wind.temperatureCelsius?.let { temperature ->
            DetailRow(
                label = stringResource(R.string.home_detail_temperature),
                value = "${temperature.roundToInt()} °C",
            )
        }
    }
}

@Composable
private fun WindDetails(
    relativeWind: RelativeWind,
    session: WindSessionState,
    unit: SpeedUnit,
) {
    val headwind = relativeWind.headwindMetersPerSecond
    val crosswind = relativeWind.crosswindMetersPerSecond
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        DetailRow(
            label = stringResource(R.string.home_detail_wind),
            value = "${unit.format(relativeWind.windSpeedMetersPerSecond)} " +
                "(${relativeWind.windDirectionFromDegrees.toCardinalPoint()})",
        )
        relativeWind.gustMetersPerSecond?.takeIf { session.preferences.showGusts }?.let { gust ->
            DetailRow(
                label = stringResource(R.string.home_detail_gusts),
                value = unit.format(gust),
            )
        }
        DetailRow(
            label = stringResource(
                if (headwind >= 0f) R.string.home_detail_headwind else R.string.home_detail_tailwind,
            ),
            value = unit.format(abs(headwind)),
        )
        DetailRow(
            label = stringResource(R.string.home_detail_crosswind),
            value = "${unit.format(abs(crosswind))} " + stringResource(
                if (crosswind >= 0f) R.string.home_detail_from_right else R.string.home_detail_from_left,
            ),
        )
        DetailRow(
            label = stringResource(R.string.home_detail_heading),
            value = "${relativeWind.headingDegrees.roundToInt()}° " +
                relativeWind.headingDegrees.toCardinalPoint(),
        )
        session.movement?.let { movement ->
            DetailRow(
                label = stringResource(R.string.home_detail_speed),
                value = unit.format(movement.speedMetersPerSecond),
            )
        }
        session.wind?.temperatureCelsius?.let { temperature ->
            DetailRow(
                label = stringResource(R.string.home_detail_temperature),
                value = "${temperature.roundToInt()} °C",
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun BubbleCard(
    isRunning: Boolean,
    canStart: Boolean,
    onBubbleToggled: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_bubble_card_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.home_bubble_card_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = isRunning,
                onCheckedChange = onBubbleToggled,
                enabled = canStart || isRunning,
            )
        }
    }
}

@Composable
private fun PermissionsSection(
    uiState: HomeUiState,
    onRequestLocation: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestNotifications: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!uiState.hasLocationPermission) {
            PermissionCard(
                title = stringResource(R.string.permission_location_title),
                rationale = stringResource(R.string.permission_location_rationale),
                action = stringResource(R.string.permission_location_action),
                onClick = onRequestLocation,
            )
        }
        if (!uiState.hasOverlayPermission) {
            PermissionCard(
                title = stringResource(R.string.permission_overlay_title),
                rationale = stringResource(R.string.permission_overlay_rationale),
                action = stringResource(R.string.permission_overlay_action),
                onClick = onRequestOverlay,
            )
        }
        if (!uiState.hasNotificationPermission) {
            PermissionCard(
                title = stringResource(R.string.permission_notifications_title),
                rationale = stringResource(R.string.permission_notifications_rationale),
                action = stringResource(R.string.permission_notifications_action),
                onClick = onRequestNotifications,
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    rationale: String,
    action: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = rationale, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun WindSessionError.message(): String = when (this) {
    WindSessionError.MissingLocationPermission -> stringResource(R.string.permission_location_title)
    WindSessionError.LocationUnavailable -> stringResource(R.string.error_location_unavailable)
    is WindSessionError.WindFetchFailed -> stringResource(R.string.error_wind_fetch, reason)
}
