package com.example.nearbywallpaper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

/**
 * Runs periodically (via WorkManager). Grabs current location, then
 * refreshes the wallpaper from a nearby place photo.
 */
class WallpaperWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Permission may have been revoked since scheduling.
        val fine = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return Result.failure()

        val location = try {
            val client = LocationServices.getFusedLocationProviderClient(appContext)
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                ?: client.lastLocation.await()
        } catch (e: SecurityException) {
            return Result.failure()
        } catch (e: Exception) {
            return Result.retry()
        } ?: return Result.retry()

        val result = WallpaperRepository(appContext)
            .refreshWallpaper(location.latitude, location.longitude)

        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "nearby_wallpaper_refresh"
    }
}
