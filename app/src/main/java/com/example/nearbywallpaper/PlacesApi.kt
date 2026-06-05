package com.example.nearbywallpaper

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ---- Response models (only the fields we need) ----

data class NearbyResponse(val results: List<NearbyResult> = emptyList())
data class NearbyResult(val photos: List<PhotoRef> = emptyList(), val name: String? = null)
data class PhotoRef(val photo_reference: String)

interface PlacesService {
    @GET("maps/api/place/nearbysearch/json")
    suspend fun nearby(
        @Query("location") location: String,   // "lat,lng"
        @Query("radius") radius: Int,
        @Query("type") type: String,
        @Query("key") key: String
    ): NearbyResponse
}

object PlacesApi {
    private const val BASE = "https://maps.googleapis.com/"

    val service: PlacesService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlacesService::class.java)
    }

    // The Photo endpoint returns the image bytes directly (after a redirect).
    // We just build the URL and let the downloader fetch it.
    fun photoUrl(ref: String, maxWidth: Int = 1080): String =
        "${BASE}maps/api/place/photo?maxwidth=$maxWidth" +
            "&photo_reference=$ref&key=${BuildConfig.MAPS_API_KEY}"
}
