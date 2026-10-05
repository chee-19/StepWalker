package com.cheewei.stepwalker.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.ZonedDateTime

/** Read-only access to Health Connect's aggregate total, without filtering sources. */
class HealthConnectStepsReader(context: Context) {
    private val context = context.applicationContext
    private val client by lazy { HealthConnectClient.getOrCreate(this.context) }

    fun availability(): Int = HealthConnectClient.getSdkStatus(context)

    suspend fun hasPermission(): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)

    suspend fun readTodaySteps(): Long {
        val now = ZonedDateTime.now()
        val midnight = now.toLocalDate().atStartOfDay(now.zone)
        val result = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(midnight.toInstant(), now.toInstant())
            )
        )
        // An empty aggregate means no steps are available for this time range.
        return result[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    companion object {
        val PERMISSIONS = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    }
}
