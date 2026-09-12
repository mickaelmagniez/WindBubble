package io.github.mickaelmagniez.windbubble.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** True when the app may draw the bubble on top of other apps. */
fun Context.canDrawOverlay(): Boolean = Settings.canDrawOverlays(this)

/** Intent opening the system screen where the overlay permission is granted. */
fun Context.overlaySettingsIntent(): Intent = Intent(
    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
    Uri.fromParts("package", packageName, null),
)
