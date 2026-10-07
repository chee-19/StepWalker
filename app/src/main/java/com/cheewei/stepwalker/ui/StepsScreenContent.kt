package com.cheewei.stepwalker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cheewei.stepwalker.data.DailySteps
import com.cheewei.stepwalker.data.GhostComparison
import com.cheewei.stepwalker.data.GhostStatus
import com.cheewei.stepwalker.data.StepsSnapshot
import com.cheewei.stepwalker.ui.theme.StepWalkerTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private fun stepsText(steps: Long): String = NumberFormat.getIntegerInstance().format(steps)

// Local race accents keep the status recognizable even with Android dynamic colors.
@Composable
private fun raceAccent(status: GhostStatus): Color = when (status) {
    GhostStatus.AHEAD -> if (MaterialTheme.colorScheme.background.luminanceForRace() < 0.5f)
        Color(0xFF8BDEAD) else Color(0xFF23784D)
    GhostStatus.BEHIND -> if (MaterialTheme.colorScheme.background.luminanceForRace() < 0.5f)
        Color(0xFFFFBD94) else Color(0xFFAD5327)
    GhostStatus.TIED -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun Color.luminanceForRace(): Float = (red + green + blue) / 3f

/** Pure display content: runtime and previews use the same layout. */
@Composable
fun StepsScreenContent(
    snapshot: StepsSnapshot,
    dailyGoal: Long = DEFAULT_DAILY_STEP_GOAL,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StepHeader()
            TodayStepCounter(snapshot.ghost.todaySteps)
            RaceStatus(snapshot.ghost)
            RaceCard(snapshot.ghost)
            YesterdayComparisonCard(snapshot.ghost.yesterdaySameTimeSteps)
            DailyGoalCard(snapshot.ghost.todaySteps, dailyGoal)
            if (snapshot.ghost.todaySteps == 0L) {
                Text("No steps available yet. Check your watch sync and Health Connect data.",
                    style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh Garmin steps")
            }
            StepHistory(snapshot.history)
            Text("Garmin steps · From local midnight to the last refresh.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StepHeader() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text("STEPWALKER", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text("YOU vs YOU", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TodayStepCounter(steps: Long) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TODAY", style = MaterialTheme.typography.labelLarge, letterSpacing = 3.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stepsText(steps), style = MaterialTheme.typography.displayLarge,
            fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-2).sp)
        Text("steps", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RaceStatus(ghost: GhostComparison) {
    val accent = raceAccent(ghost.status)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = accent.copy(alpha = 0.10f)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(when (ghost.status) {
                GhostStatus.AHEAD -> "YOU'RE AHEAD"
                GhostStatus.BEHIND -> "YOUR GHOST IS AHEAD"
                GhostStatus.TIED -> "NECK AND NECK"
            }, color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(when (ghost.status) {
                GhostStatus.AHEAD -> "+${stepsText(ghost.difference)} steps"
                GhostStatus.BEHIND -> "${stepsText(-ghost.difference)} steps behind"
                GhostStatus.TIED -> "You're tied"
            }, color = accent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/** Character slots can later supply custom artwork without changing the track or positions. */
@Composable
fun RaceCard(
    ghost: GhostComparison,
    modifier: Modifier = Modifier,
    youCharacter: @Composable () -> Unit = { RacerMarker(isGhost = false, color = raceAccent(ghost.status)) },
    ghostCharacter: @Composable () -> Unit = { RacerMarker(isGhost = true, color = MaterialTheme.colorScheme.onSurfaceVariant) }
) {
    val accent = raceAccent(ghost.status)
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("The daily rematch", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Every step moves you forward.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            ScrollableRaceTrack(ghost, accent,
                youCharacter = youCharacter, ghostCharacter = ghostCharacter)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("You", style = MaterialTheme.typography.labelMedium, color = accent)
                    Text(stepsText(ghost.todaySteps), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Yesterday", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stepsText(ghost.yesterdaySameTimeSteps), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            Text("Same time. A new chance.", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RacerMarker(isGhost: Boolean, color: Color) {
    Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.12f)) {
        Canvas(Modifier.padding(8.dp)) {
            val w = size.width
            val h = size.height
            if (isGhost) {
                val body = Path().apply {
                    moveTo(w * .16f, h * .9f)
                    lineTo(w * .16f, h * .4f)
                    cubicTo(w * .16f, -h * .06f, w * .84f, -h * .06f, w * .84f, h * .4f)
                    lineTo(w * .84f, h * .9f)
                    lineTo(w * .66f, h * .76f)
                    lineTo(w * .5f, h * .9f)
                    lineTo(w * .34f, h * .76f)
                    close()
                }
                drawPath(body, color)
                drawCircle(Color.White, w * .055f, Offset(w * .38f, h * .4f))
                drawCircle(Color.White, w * .055f, Offset(w * .62f, h * .4f))
            } else {
                drawCircle(color, w * .12f, Offset(w * .55f, h * .14f))
                fun limb(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(color,
                    Offset(w * x1, h * y1), Offset(w * x2, h * y2), w * .10f, StrokeCap.Round)
                limb(.52f, .34f, .43f, .62f)
                limb(.50f, .37f, .27f, .52f)
                limb(.50f, .37f, .74f, .49f)
                limb(.43f, .62f, .23f, .89f)
                limb(.43f, .62f, .72f, .87f)
            }
        }
    }
}

@Composable
fun YesterdayComparisonCard(steps: Long) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Yesterday at this time", style = MaterialTheme.typography.titleMedium)
            Text("${stepsText(steps)} steps", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Your past self, from midnight to the same local time.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DailyGoalCard(steps: Long, dailyGoal: Long) {
    val goal = DailyStepGoal(steps, dailyGoal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Daily goal · ${goal.percentage}%", style = MaterialTheme.typography.titleSmall)
        LinearProgressIndicator(goal.progress, Modifier.fillMaxWidth(), color = raceAccent(GhostStatus.AHEAD))
        Text("${stepsText(steps)} / ${stepsText(dailyGoal)} steps", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StepHistory(history: List<DailySteps>) {
    if (history.isEmpty()) return
    val dates = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    val today = history.last().date
    val total = history.sumOf { it.steps }
    val average = (total.toDouble() / history.size).roundToLong()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your last 7 days", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            history.forEach { day ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (day.date == today) "Today" else day.date.format(dates), Modifier.weight(1f))
                    Text("${stepsText(day.steps)} steps", fontWeight = FontWeight.Medium)
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("7-day total: ${stepsText(total)} steps", style = MaterialTheme.typography.titleSmall)
            Text("Daily average: ${stepsText(average)} steps", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun sampleSnapshot(todaySteps: Long, yesterdaySteps: Long): StepsSnapshot {
    val today = LocalDate.of(2026, 10, 7)
    val counts = listOf(6200L, 8100L, 5400L, 7200L, 9100L, 6800L, todaySteps)
    return StepsSnapshot(counts.mapIndexed { index, count -> DailySteps(today.minusDays((6 - index).toLong()), count) },
        GhostComparison(todaySteps, yesterdaySteps))
}

@Preview(name = "01 · Close race", showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
fun StepsScreenAheadPreview() {
    StepWalkerTheme(darkTheme = false, dynamicColor = false) {
        StepsScreenContent(sampleSnapshot(4250, 3810))
    }
}

@Preview(name = "02 · Ghost far ahead", showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
fun StepsScreenBehindPreview() {
    StepWalkerTheme(darkTheme = false, dynamicColor = false) {
        StepsScreenContent(sampleSnapshot(2100, 7800))
    }
}

@Preview(name = "03 · Neck and neck", showBackground = true, widthDp = 390, heightDp = 1100)
@Composable
fun StepsScreenTiedPreview() {
    StepWalkerTheme(darkTheme = false, dynamicColor = false) {
        StepsScreenContent(sampleSnapshot(4000, 4000))
    }
}

@Preview(name = "04 · Dark / small phone", showBackground = true, widthDp = 320, heightDp = 900)
@Composable
fun StepsScreenDarkPreview() {
    StepWalkerTheme(darkTheme = true, dynamicColor = false) {
        StepsScreenContent(sampleSnapshot(4250, 3810))
    }
}
