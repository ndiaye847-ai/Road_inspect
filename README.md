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

## Known limitations (inherited from the original project)

- Targets an old Android SDK (compileSdkVersion 22, built with Gradle
  plugin 2.1.3) and has not yet been modernized.
- Uses `jcenter()`, which has been shut down; dependency resolution will
  need to be migrated to `mavenCentral()` / `google()` before this
  builds with current tooling.
