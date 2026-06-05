package com.example.nearbywallpaper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.example.nearbywallpaper.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val mainHandler = Handler(Looper.getMainLooper())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            refreshNow()
        } else {
            result("Location permission denied. Can't fetch nearby photos.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRefresh.setOnClickListener { ensurePermissionThen { refreshNow() } }
        binding.btnSchedule.setOnClickListener { schedulePeriodic() }

        if (!BuildConfig.MAPS_API_KEY.startsWith("AIza")) {
            status("⚠ Add your Maps API key in app/build.gradle.kts first.")
        }
    }

    private fun ensurePermissionThen(block: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) block() else permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun refreshNow() {
        status("Locating…")
        lifecycleScope.launch {
            try {
                val client = LocationServices.getFusedLocationProviderClient(this@MainActivity)
                val loc = client.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY, null
                ).await() ?: client.lastLocation.await()

                if (loc == null) {
                    result("Couldn't get location. Try again outdoors / with GPS on.")
                    return@launch
                }
                status("Fetching nearby photos…")
                // Setting the wallpaper recreates this Activity (a system wallpaper-colors
                // config change), which cancels this coroutine — so code after this call
                // may never run. The callback fires on the repository's IO thread and shows
                // a Toast via the application context, which survives the recreation.
                WallpaperRepository(applicationContext)
                    .refreshWallpaper(loc.latitude, loc.longitude) { msg -> toast(msg) }
            } catch (e: SecurityException) {
                result("Location permission missing.")
            } catch (e: Exception) {
                result("Error: ${e.message}")
            }
        }
    }

    private fun schedulePeriodic() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        // Minimum interval Android allows for periodic work is 15 minutes.
        val request = PeriodicWorkRequestBuilder<WallpaperWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WallpaperWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
        status("Scheduled: wallpaper refreshes ~hourly.")
    }

    private fun status(msg: String) { binding.txtStatus.text = msg }

    /** Toast via the application context so it survives the Activity recreation that
     *  setting the wallpaper triggers. Safe to call from any thread. */
    private fun toast(msg: String) {
        mainHandler.post { Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show() }
    }

    /** Terminal feedback: shown both inline and as a Toast (the Toast survives recreation). */
    private fun result(msg: String) {
        toast(msg)
        status(msg)
    }
}
