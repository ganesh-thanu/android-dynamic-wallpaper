# Nearby Wallpaper

An Android app that fetches photos of places near your current location from
the Google Places API and sets one as your device wallpaper — manually or on an
hourly auto-refresh.

## What it does
- Gets your current location (FusedLocationProvider)
- Calls Places **Nearby Search** for tourist attractions within 1.5 km
- Downloads a place photo and sets it as the system wallpaper
- Rotates to a new photo on each refresh
- Optional hourly background refresh via WorkManager

## Setup (5 minutes)

1. **Install Android Studio** (Hedgehog or newer): https://developer.android.com/studio
2. **Unzip** this project and open the folder in Android Studio.
   Let it sync Gradle (it will download the correct Gradle + wrapper jar).
3. **Get a Google API key**
   - Go to https://console.cloud.google.com
   - Create a project, then enable **Places API**
   - Create an API key (Credentials → Create credentials → API key)
   - Restrict it to Android apps using your package name
     (`com.example.nearbywallpaper`) + your debug signing SHA-1
4. **Paste the key** into `app/build.gradle.kts`:
   ```
   buildConfigField("String", "MAPS_API_KEY", "\"YOUR_KEY_HERE\"")
   ```
5. **Run** on a device/emulator (▶). Grant location permission.
   Tap **Set wallpaper from nearby places**.

## Security warning
The key is compiled into the APK (BuildConfig). This is fine for personal use,
but anyone with the APK can extract it. For a published app, proxy the Places
calls through your own backend and never ship the key. Always restrict the key
in Cloud Console and set a billing budget/quota cap.

## Notes & limitations
- Android's minimum periodic-work interval is **15 minutes**; the app uses 1 hour.
- Setting the wallpaper from the background requires the screen-on /
  battery-optimization conditions WorkManager allows; refreshes are best-effort.
- Places photo downloads count against your API quota and billing — watch usage.
- "Nearby" uses type `tourist_attraction`. Change `type`/`radius` in
  `WallpaperRepository.kt` to tune results.

## Project structure
```
app/src/main/java/com/example/nearbywallpaper/
  MainActivity.kt          UI, permissions, manual refresh, scheduling
  PlacesApi.kt             Retrofit service + photo URL builder
  WallpaperRepository.kt   Fetch nearby photos, download, set wallpaper
  WallpaperWorker.kt       Background periodic refresh
```
