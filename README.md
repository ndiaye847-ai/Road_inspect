# Road_inspect

Mobile application for road surface quality measurement.

This project is a fork of [RoadLab-Pro](https://github.com/WorldBank-Transport/RoadLab-Pro)
(World Bank Transport Global Practice), licensed under the
[Apache License 2.0](LICENSE). See [NOTICE](NOTICE) for a summary of
changes made in this fork.

## Before building

The original hardcoded secrets (signing keystore, Google OAuth client
secret, Google Maps API key) were removed for security. You must supply
your own before the app will build/run fully:

1. **Signing keystore** — generate one with `keytool -genkey` and set
   `RELEASE_KEYSTORE_FILE`, `RELEASE_KEYSTORE_PASSWORD`,
   `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` (and the `DEBUG_*`
   equivalents) as environment variables.
2. **Google OAuth client** — create one in
   [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
   and set `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` environment
   variables.
3. **Google Maps API key** — generate one at
   [Google Maps Platform](https://console.cloud.google.com/google/maps-apis)
   and replace the placeholder in
   `app/src/main/res/values/strings.xml` (`google_maps_key`).
4. **Dropbox app key** (optional, for Dropbox sync) — the app key in
   `Constants.DROPBOX_APPLICATION_KEY` and the `db-...` URI scheme in
   `AndroidManifest.xml` belong to the original app; register your own
   Dropbox app if you want working Dropbox sync.

## Build system modernization (done)

The Gradle/AGP toolchain has been updated from the 2016-era originals:

- Gradle 2.14.1 → 8.9, Android Gradle Plugin 2.1.3 → 8.5.2.
- `jcenter()` (shut down in 2022) replaced with `mavenCentral()` +
  `https://maven.google.com`, plus `jitpack.io` as a fallback for two
  libraries never republished elsewhere (see below).
- `compileSdk`/`targetSdk` 22 → 34, `minSdk` 19 → 21 (required by current
  Play Services), Java compatibility set to 1.8.
- Android Support Library → AndroidX (`appcompat`, `legacy-support-v4`,
  `viewpager`, `drawerlayout`) across all ~40 affected files, including
  layout XML tags.
- Retrofit 2.0-beta2 → 2.11.0 (stable): callback signatures updated
  (`onResponse(Call, Response)` / `onFailure(Call, Throwable)`), OkHttp
  v2 (`com.squareup.okhttp`) → OkHttp3 (transitive via Retrofit), the
  SSL/hostname-verification bypass in `RestClient` rebuilt for OkHttp3's
  immutable `OkHttpClient.Builder`.
- Google Sign-In rewritten from the deprecated `GoogleApiClient` +
  `Auth.GOOGLE_SIGN_IN_API` pattern to the current `GoogleSignInClient`
  API (`GoogleAPIHelper.java`). Drive file access itself was already
  implemented as plain REST calls via Retrofit, not the old Drive
  Android API, so it needed no changes.
- Removed dependencies that were declared but never referenced in code:
  `play-services-drive`, `play-services-location`, the local
  `commons-net-3.3.jar`. Local `opencsv-3.2.jar` replaced with the
  `com.opencsv:opencsv:5.9` Maven artifact (same `com.opencsv.*` package,
  same `CSVWriter` constructors already used in the code).
- `AndroidPlot` 0.6.1 → `androidplot-core` 1.5.11.
- Fixed APIs removed from the platform since this project was written:
  `org.apache.http.*` (restored via `useLibrary 'org.apache.http.legacy'`
  for the dead HockeyApp crash sender), `WebSettings.setAppCacheEnabled`/
  `setDefaultZoom` (removed calls; `LoginGoogleDialog` isn't even
  instantiated anywhere in the app).
- `AndroidManifest.xml`: removed the `package` attribute (replaced by
  `namespace` in `build.gradle`, required by AGP 8), added the
  `android:exported` attribute now mandatory (API 31+) on components
  with intent filters, scoped `WRITE_EXTERNAL_STORAGE`/
  `READ_EXTERNAL_STORAGE` with `maxSdkVersion` for scoped storage,
  dropped the long-removed `USE_CREDENTIALS` permission.

**This was done without being able to compile-verify it end-to-end** —
this sandbox's network policy blocks `dl.google.com`, which every
Android Gradle Plugin / AndroidX / Play Services artifact resolves
through even via `maven.google.com`, so `./gradlew assembleDebug` could
not be run here. Build it in Android Studio (or CI with normal internet
access) and fix anything this review missed.

## Runtime permissions and TLS (done)

- **Runtime permission requests added** (`MainActivity`): on launch the
  app now requests `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`,
  `RECORD_AUDIO` and `GET_ACCOUNTS` via `ActivityCompat.requestPermissions`
  if not already granted, and defers GPS setup (`GPSDetector.init()`,
  which calls `requestLocationUpdates` and would otherwise throw
  `SecurityException` on a fresh install) until they're granted -
  `onRequestPermissionsResult` calls `recreate()` on success so the
  normal `onCreate` flow re-runs with permissions now in place. A
  `Toast` (new string `permissions_required_toast`) tells the user if
  they refuse. Voice recording (`VoiceRecordDialog`/`VoiceRecordPlayer`)
  wasn't given its own guard — it's reached deep enough into the UI that
  the startup prompt will normally have been resolved by then, but this
  wasn't verified and is worth a dedicated check before shipping.
- **TLS/hostname verification re-enabled** (`RestClient`): the old
  `getUnsafeOkHttpClient()` built an `OkHttpClient` with a trust-all
  `X509TrustManager` and a `HostnameVerifier` that accepted every host -
  every HTTPS request this app made was vulnerable to a
  man-in-the-middle attack, regardless of this fork. It's replaced with
  a plain `OkHttpClient.Builder()` that uses the platform's default
  trust store and hostname verification, keeping only the existing
  request/response logging interceptor.

## Known follow-ups (not done)

- **Two dependencies are unverified jitpack.io coordinates**
  (`app/build.gradle`): `com.github.JoanZapata:android-pdfview` and
  `com.github.nostra13:Android-Universal-Image-Loader`. Neither library
  was ever republished to Maven Central/Google after JCenter shut down,
  and this sandbox's network policy blocks jitpack.io itself (not just
  `dl.google.com`), so these could not be checked from here either.
  Confirm the exact available tag on jitpack.io before building, or
  replace them with actively maintained alternatives (e.g. AndroidPdfViewer
  for PDF rendering, Glide/Coil for image loading).
- **`androidTest/KMLGenerationTest.java`** uses the JUnit3-style
  `android.test.ApplicationTestCase`, part of the legacy Android testing
  framework that needs `useLibrary 'android.test.runner'`/
  `'android.test.base'` or a rewrite to AndroidX Test + JUnit4. This
  doesn't block `assembleDebug` (only instrumented test compilation).
- Scoped storage (Android 10+) and granular media permissions (Android
  13+) may require changes to the file-export/import code paths that
  use raw `java.io.File` paths under external storage.
