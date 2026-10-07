package com.cheewei.stepwalker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.cheewei.stepwalker.data.GhostComparison
import kotlinx.coroutines.launch
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.roundToInt

private val CHARACTER_WIDTH = 80.dp
private val TRACK_EDGE_PADDING = CHARACTER_WIDTH / 2 + 8.dp
private val TRACK_Y = 92.dp

@Composable
fun ScrollableRaceTrack(
    comparison: GhostComparison,
    accent: Color,
    modifier: Modifier = Modifier,
    youCharacter: @Composable () -> Unit,
    ghostCharacter: @Composable () -> Unit
) {
    val positions = racePositions(comparison.todaySteps, comparison.yesterdaySameTimeSteps)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val rail = MaterialTheme.colorScheme.outlineVariant
    val ghostColor = MaterialTheme.colorScheme.onSurfaceVariant
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val viewportPx = with(density) { maxWidth.toPx() }
        val youPx = with(density) { (TRACK_EDGE_PADDING + positions.you.dp).toPx() }
        val ghostPx = with(density) { (TRACK_EDGE_PADDING + positions.ghost.dp).toPx() }
        val trackWidth = positions.trackLengthDp.dp + TRACK_EDGE_PADDING * 2
        val maximumScroll = with(density) { trackWidth.toPx() - viewportPx }.coerceAtLeast(0f)
        fun centered(positionPx: Float) =
            (positionPx - viewportPx / 2).coerceIn(0f, maximumScroll).roundToInt()
        // Start around YOU, including in static previews. Refreshes preserve manual exploration.
        val scroll = rememberScrollState(initial = centered(youPx))
        val closeRace = abs(positions.you - positions.ghost) < CHARACTER_WIDTH.value + 12f
        val ghostTop = if (closeRace) 112.dp else 10.dp
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
                    Box(Modifier.width(trackWidth).height(210.dp)) {
                        Canvas(Modifier.fillMaxSize()) {
                            val start = TRACK_EDGE_PADDING.toPx()
                            val end = start + positions.trackLengthDp.dp.toPx()
                            val y = TRACK_Y.toPx()
                            drawLine(rail, Offset(start, y), Offset(end, y), 5.dp.toPx(), StrokeCap.Round)
                            drawLine(accent.copy(alpha = .4f), Offset(start, y),
                                Offset(youPx, y), 5.dp.toPx(), StrokeCap.Round)
                            // Both connectors meet exact step coordinates on this single rail.
                            drawLine(accent, Offset(youPx, 74.dp.toPx()), Offset(youPx, y), 2.dp.toPx())
                            drawLine(ghostColor, Offset(ghostPx, if (closeRace) ghostTop.toPx() else 74.dp.toPx()),
                                Offset(ghostPx, y), 2.dp.toPx())
                            drawCircle(accent, 5.dp.toPx(), Offset(youPx, y))
                            drawCircle(ghostColor, 3.dp.toPx(), Offset(ghostPx, y))
                            for (thousand in 0..(positions.endSteps / TRACK_MARKER_INTERVAL_STEPS)) {
                                val x = start + stepPosition(thousand * TRACK_MARKER_INTERVAL_STEPS).dp.toPx()
                                drawLine(rail, Offset(x, y + 8.dp.toPx()), Offset(x, y + 16.dp.toPx()), 2.dp.toPx())
                            }
                        }
                        RaceCharacter("YOU", comparison.todaySteps, accent,
                            Modifier.absoluteOffset(x = TRACK_EDGE_PADDING + positions.you.dp - CHARACTER_WIDTH / 2, y = 10.dp),
                            youCharacter)
                        RaceCharacter("GHOST", comparison.yesterdaySameTimeSteps, ghostColor,
                            Modifier.absoluteOffset(x = TRACK_EDGE_PADDING + positions.ghost.dp - CHARACTER_WIDTH / 2, y = ghostTop),
                            ghostCharacter)
                        for (thousand in 0..(positions.endSteps / TRACK_MARKER_INTERVAL_STEPS)) {
                            TrackMarker(thousand * TRACK_MARKER_INTERVAL_STEPS,
                                Modifier.absoluteOffset(x = TRACK_EDGE_PADDING + stepPosition(thousand * TRACK_MARKER_INTERVAL_STEPS).dp - 24.dp,
                                    y = 182.dp).width(48.dp))
                        }
                    }
                }
            }
            val left = scroll.value.toFloat()
            val right = left + viewportPx
            val halfCharacter = with(density) { (CHARACTER_WIDTH / 2).toPx() }
            val numbers = NumberFormat.getIntegerInstance()
            fun indicator(name: String, positionPx: Float, gap: Long): String? {
                val direction = when {
                    positionPx - halfCharacter < left -> -1
                    positionPx + halfCharacter > right -> 1
                    else -> return null
                }
                val relative = when {
                    gap > 0 -> "${numbers.format(gap)} steps ahead"
                    gap < 0 -> "${numbers.format(-gap)} steps behind"
                    else -> "tied"
                }
                return if (direction < 0) "← $name · $relative" else "$name · $relative →"
            }
            indicator("You", youPx, comparison.difference)?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = accent)
            }
            indicator("Ghost", ghostPx, -comparison.difference)?.let {
                Text(it, style = MaterialTheme.typography.labelMedium, color = ghostColor)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { scope.launch { scroll.animateScrollTo(centered(youPx)) } },
                    modifier = Modifier.weight(1f)) { Text("Find Me") }
                OutlinedButton(onClick = { scope.launch { scroll.animateScrollTo(centered(ghostPx)) } },
                    modifier = Modifier.weight(1f)) { Text("Find Ghost") }
            }
            Text("Swipe to explore the race", style = MaterialTheme.typography.bodySmall, color = ghostColor)
        }
    }
}

/** Artwork can be replaced while keeping the exact coordinate and accessible step count. */
@Composable
fun RaceCharacter(
    name: String,
    steps: Long,
    color: Color,
    modifier: Modifier = Modifier,
    artwork: @Composable () -> Unit
) {
    Column(modifier.width(CHARACTER_WIDTH).semantics {
        contentDescription = "$name at ${NumberFormat.getIntegerInstance().format(steps)} steps"
    }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        Box(Modifier.size(44.dp)) { artwork() }
    }
}

@Composable
fun TrackMarker(steps: Long, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(if (steps == 0L) "0" else "${steps / TRACK_MARKER_INTERVAL_STEPS}k",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
