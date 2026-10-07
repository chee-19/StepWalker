package com.cheewei.stepwalker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.cheewei.stepwalker.data.HealthConnectStepsReader
import com.cheewei.stepwalker.data.StepsSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.ZonedDateTime

private sealed interface StepsState {
    object Loading : StepsState
    data class Unavailable(val needsUpdate: Boolean) : StepsState
    object PermissionRequired : StepsState
    data class Loaded(val snapshot: StepsSnapshot) : StepsState
    object Error : StepsState
}

@Composable
fun StepsScreen(reader: HealthConnectStepsReader, dailyGoal: Long = DEFAULT_DAILY_STEP_GOAL) {
    require(dailyGoal > 0) { "The daily step goal must be positive." }
    var state by remember { mutableStateOf<StepsState>(StepsState.Loading) }
    var refresh by remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { refresh++ }

    // Recheck on resume/manual refresh; poll gently only while this screen is resumed.
    LaunchedEffect(reader, lifecycleOwner, refresh) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            state = StepsState.Loading
            while (isActive) {
                val queryDay = ZonedDateTime.now().toLocalDate()
                state = try {
                    when (reader.availability()) {
                        HealthConnectClient.SDK_AVAILABLE -> {
                            if (reader.hasPermission()) StepsState.Loaded(reader.readSnapshot())
                            else StepsState.PermissionRequired
                        }
                        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                            StepsState.Unavailable(needsUpdate = true)
                        else -> StepsState.Unavailable(needsUpdate = false)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: SecurityException) {
                    StepsState.PermissionRequired
                } catch (_: Exception) {
                    StepsState.Error
                }
                val now = ZonedDateTime.now()
                // A request spanning midnight must not leave yesterday's result on screen.
                if (now.toLocalDate() != queryDay) {
                    state = StepsState.Loading
                    continue
                }
                val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
                delay(minOf(60_000L, Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)))
                if (ZonedDateTime.now().toLocalDate() != queryDay) state = StepsState.Loading
            }
        }
    }

    val current = state
    if (current is StepsState.Loaded) {
        StepsScreenContent(current.snapshot, dailyGoal, onRefresh = { refresh++ })
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StepHeader()
        when (current) {
            StepsState.Loading -> {
                Text("Loading your daily rematch...")
            }
            is StepsState.Unavailable -> {
                Text(if (current.needsUpdate)
                    "Health Connect unavailable. Install or update Health Connect, then try again."
                else "Health Connect unavailable on this device.")
                Button(onClick = { refresh++ }) { Text("Try again") }
            }
            StepsState.PermissionRequired -> {
                Text("Permission not granted. Allow StepWalker to read steps in Health Connect.")
                Button(onClick = {
                    try {
                        permissionLauncher.launch(HealthConnectStepsReader.PERMISSIONS)
                    } catch (_: Exception) {
                        state = StepsState.Error
                    }
                }) { Text("Allow reading steps") }
                Text("If the permission prompt no longer appears, enable Steps for StepWalker in Health Connect settings.")
            }
            StepsState.Error -> {
                Text("Error reading step data. Check Health Connect and try again.")
                Button(onClick = { refresh++ }) { Text("Try again") }
            }
            is StepsState.Loaded -> Unit
        }
    }
}
