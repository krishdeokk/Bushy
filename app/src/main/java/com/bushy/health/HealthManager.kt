package com.bushy.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class HealthManager(private val context: Context) {

    private val healthConnectClient by lazy {
        val status = HealthConnectClient.getSdkStatus(context)
        if (status == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    fun getSdkStatus(): Int = HealthConnectClient.getSdkStatus(context)

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    suspend fun hasPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun readDailySteps(): Long {
        val client = healthConnectClient ?: return 0
        try {
            // More robust "Start of Day" calculation
            val now = Instant.now()
            val startTime = ZonedDateTime.now().toLocalDate().atStartOfDay(ZonedDateTime.now().zone).toInstant()
            
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, now)
                )
            )
            return response[StepsRecord.COUNT_TOTAL] ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }
}
