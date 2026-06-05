package com.example.nearbywallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches nearby place photos and sets one as the system wallpaper.
 * Keeps a rotating index in SharedPreferences so each refresh shows a new photo.
 */
class WallpaperRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("nw_prefs", Context.MODE_PRIVATE)

    /**
     * @param notify optional callback invoked (on this IO thread) with a user-facing
     *   message once the outcome is known. Setting the wallpaper recreates the calling
     *   Activity, cancelling its coroutine, so the UI can't reliably react to the return
     *   value — the callback lets it report success/failure regardless. The background
     *   worker omits it so it stays silent.
     */
    suspend fun refreshWallpaper(
        lat: Double,
        lng: Double,
        notify: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val outcome: Result<String> = try {
            val resp = PlacesApi.service.nearby(
                location = "$lat,$lng",
                radius = 1500,
                type = "tourist_attraction",
                key = BuildConfig.MAPS_API_KEY
            )

            val refs = resp.results
                .flatMap { it.photos }
                .map { it.photo_reference }
                .distinct()

            if (refs.isEmpty()) {
                Result.failure(Exception("No nearby photos found"))
            } else {
                // Rotate through available photos on each refresh.
                val idx = prefs.getInt("photo_index", 0) % refs.size
                prefs.edit().putInt("photo_index", idx + 1).apply()

                val bitmap = downloadBitmap(PlacesApi.photoUrl(refs[idx]))
                if (bitmap == null) {
                    Result.failure(Exception("Failed to decode image"))
                } else {
                    WallpaperManager.getInstance(context).setBitmap(bitmap)
                    Result.success("Wallpaper updated (${refs.size} photos nearby)")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        notify?.invoke(outcome.getOrElse { "Error: ${it.message}" })
        outcome
    }

    private fun downloadBitmap(urlStr: String): Bitmap? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 15000
            }
            conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }
}
