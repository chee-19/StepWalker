package com.cheewei.stepwalker.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime

/** Read-only access to Health Connect's aggregate total. */
class HealthConnectStepsReader(context: Context) {
    private val context = context.applicationContext
    private val client by lazy { HealthConnectClient.getOrCreate(this.context) }

    fun availability(): Int = HealthConnectClient.getSdkStatus(context)

    suspend fun hasPermission(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)

    suspend fun readTodaySteps(): Long {
        val now = ZonedDateTime.now()
        val midnight = now.toLocalDate().atStartOfDay(now.zone)
        return readGarminSteps(midnight.toInstant(), now.toInstant())
    }

    suspend fun readLastSevenDaysSteps(): List<DailySteps> {
        val now = ZonedDateTime.now()
        val today = now.toLocalDate()
        val history = mutableListOf<DailySteps>()
        for (daysAgo in 6 downTo 0) {
            val date = today.minusDays(daysAgo.toLong())
            val start = date.atStartOfDay(now.zone).toInstant()
            // Calendar boundaries respect local time-zone changes; today ends at this refresh.
            val end = if (date == today) now.toInstant()
                else date.plusDays(1).atStartOfDay(now.zone).toInstant()
            history.add(DailySteps(date, readGarminSteps(start, end)))
        }
        return history
    }

    private suspend fun readGarminSteps(start: Instant, end: Instant): Long {
        val result = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end),
                // StepWalker intentionally uses Garmin Connect as its only step source.
                dataOriginFilter = setOf(DataOrigin("com.garmin.android.apps.connectmobile"))
            )
        )
        // An empty aggregate means no steps are available for this time range.
        return result[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    companion object {
        val PERMISSIONS = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    }
}
