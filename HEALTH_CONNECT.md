# Milestone 2: today's steps and daily goal

StepWalker keeps its application ID and namespace: `com.cheewei.stepwalker`.
Only `android.permission.health.READ_STEPS` is requested. There is no write,
background-read, history-read, or activity-recognition permission.

`HealthConnectStepsReader` checks SDK availability and permission, then requests
`StepsRecord.COUNT_TOTAL` through Health Connect's aggregate API. The range is
midnight in the phone's current time zone through the time the request starts.
No source filter is applied. A missing aggregate value is displayed as zero.

The screen refreshes on resume, when Refresh is pressed, and every minute while
resumed. It also schedules the next refresh at local midnight if that comes
sooner. Refreshing stops in the background. It does not save duplicate step
data. Permission denial/revocation, provider installation/update, loading,
successful results, and read failures each have a visible state.

## Build and run

Use JDK 17 and Android Studio compatible with Android Gradle plugin 8.10.1
(Meerkat Feature Drop or newer). Sync Gradle and install SDK platform 36 if
prompted. Stable Health Connect 1.1.0 requires the updated build tooling; the
minimum SDK remains 28 and target SDK remains 34.

Build with `./gradlew :app:assembleDebug` (Windows: `.\gradlew.bat :app:assembleDebug`).
The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`.

1. Enable Developer options and USB debugging on your phone. Connect it to your
   computer, accept the debugging prompt, select the phone in Android Studio,
   and run the `app` configuration.
2. Sync the Vivoactive 5 in Garmin Connect. In Health Connect, confirm that
   Garmin Connect is allowed to write Steps and that today's step data exists.
3. Open StepWalker, tap **Allow reading steps**, and grant read access to Steps.
   The screen should show **Steps Today**, the total, the 10,000-step goal,
   percentage completed, and a progress bar.
4. Compare against today's Health Connect step total. Walk a little, sync Garmin
   again, wait for the data to reach Health Connect, and tap **Refresh**.
5. Deny or revoke StepWalker's Steps permission in Health Connect and return to
   the app: it should show **Permission not granted**. Grant it again to restore
   the result. Open the privacy-policy link in the Health Connect permission
   screen to check StepWalker's explanation.

If repeated denial prevents a new permission prompt, grant Steps through Health
Connect's App permissions settings. On Android 14+, Health Connect is integrated
into system settings; on older supported Android versions its app must be
installed and up to date.

## Limits

This reads the total supplied by Health Connect, not directly from Garmin or
the watch. Other sources and Health Connect's source priority/deduplication can
affect the result, so Garmin's own total need not match exactly. Sync can lag.
Zero can mean no data has been synced, not necessarily no walking. The displayed
number is a snapshot, refreshed each minute while visible and at midnight. Changing the
phone's time zone changes the next request's definition of today.

Real-device permission and data validation must be performed on the user's phone.
There is no racing, yesterday comparison, account, backend, or background task.

## Goal and reliability

`DailyStepGoal` defines the default of 10,000 and computes a whole-number
percentage (rounded down). The percentage can exceed 100%; the bar is clamped
to 0–1. `StepsScreen` accepts a positive `dailyGoal` parameter so a future
settings feature can supply a goal without changing the query or progress UI.

Health Connect remains the source of truth. Reopening the app, removing it from
recents, or rebooting the phone does not require restoring a sensor offset or
local counter: the app queries today's persisted Health Connect records again.
The total depends on the data that the provider has actually received; this app
does not record steps itself or make a watch sync faster.

On a physical phone, also test reopening after removal from recents and after
a reboot, leaving the app visible across midnight, and a day exceeding 10,000
steps (percentage above 100, full bar). An empty day should show zero. Check
that fresh synced steps appear within a minute while visible and immediately
on resume or Refresh. Run calculation checks with `:app:testDebugUnitTest`.
