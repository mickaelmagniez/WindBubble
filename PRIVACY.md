# Privacy Policy — Wind Bubble

Last updated: 2026-09-11

Wind Bubble is an open-source app published by the Wind Bubble contributors. It has no account, no
analytics, no advertising and no tracking library of any kind.

## What the app accesses

**Location.** The app reads your GPS position and course over ground while you use it, to compare
the wind with your direction of travel. Location is only collected while the app is open or while
the floating bubble is running, and it is never stored on a server.

**Network.** To fetch the local wind, the app sends geographic coordinates to the Open-Meteo API
(<https://open-meteo.com>). Those coordinates are **rounded to a coarse grid before being sent**, so
they identify an area, not your exact position. No identifier, account or device information is
attached to the request. Open-Meteo's own privacy policy applies to that request.

## What is stored

Your preferences (speed unit, refresh interval, bubble size, opacity and position) are stored
locally on your device only. Uninstalling the app removes them.

## What is never done

* No data is sold, shared or sent to advertisers.
* No profile, identifier or usage statistics is created.
* No location history is kept, on the device or anywhere else.

## Permissions

| Permission | Why |
| --- | --- |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Compute your heading and fetch the wind where you are |
| `INTERNET` | Query the Open-Meteo weather API |
| `SYSTEM_ALERT_WINDOW` | Draw the bubble on top of your navigation app |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION` | Keep the bubble and the GPS alive while you navigate in another app |
| `POST_NOTIFICATIONS` | Show the ongoing notification Android requires for that service |

The app does **not** request background location access: it only runs while you have started it.

## Contact

Open an issue on the project repository.
