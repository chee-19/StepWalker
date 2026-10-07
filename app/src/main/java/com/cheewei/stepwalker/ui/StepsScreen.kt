package com.cheewei.stepwalker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
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
import com.cheewei.stepwalker.data.DailySteps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.NumberFormat
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private sealed interface StepsState {
    object Loading : StepsState
    data class Unavailable(val needsUpdate: Boolean) : StepsState
    object PermissionRequired : StepsState
    data class Loaded(val history: List<DailySteps>) : StepsState {
        val steps: Long get() = history.last().steps
    }
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
                            if (reader.hasPermission()) StepsState.Loaded(reader.readLastSevenDaysSteps())
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

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("StepWalker", style = MaterialTheme.typography.headlineMedium)
        when (val current = state) {
            StepsState.Loading -> {
                Text("Loading today's steps…")
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
            is StepsState.Loaded -> {
                val goal = DailyStepGoal(current.steps, dailyGoal)
                val numbers = NumberFormat.getIntegerInstance()
                Text("Steps Today", style = MaterialTheme.typography.headlineSmall)
                Text(numbers.format(current.steps), style = MaterialTheme.typography.displayLarge)
                Text("/ ${numbers.format(dailyGoal)} steps")
                Text("${goal.percentage}%", style = MaterialTheme.typography.headlineSmall)
                LinearProgressIndicator(progress = goal.progress, modifier = Modifier.fillMaxWidth())
                Text("From local midnight to the last refresh.")
                if (current.steps == 0L) Text("No steps available yet. Check your watch sync and Health Connect data.")
                Button(onClick = { refresh++ }) { Text("Refresh") }
                StepHistory(current.history)
            }
            StepsState.Error -> {
                Text("Error reading step data. Check Health Connect and try again.")
                Button(onClick = { refresh++ }) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun StepHistory(history: List<DailySteps>) {
    val numbers = NumberFormat.getIntegerInstance()
    val dates = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    val today = history.last().date
    val total = history.sumOf { it.steps }
    val average = (total.toDouble() / history.size).roundToLong()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Step History", style = MaterialTheme.typography.headlineSmall)
        history.forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(if (day.date == today) "Today" else day.date.format(dates))
                Text("${numbers.format(day.steps)} steps")
            }
        }
        Text("7-day total: ${numbers.format(total)} steps")
        Text("Daily average: ${numbers.format(average)} steps")
    }
}
