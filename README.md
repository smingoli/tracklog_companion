# TrackLog Companion

Read-only Android companion app for browsing a TrackLog music catalog.

## Current status

The native Kotlin and Jetpack Compose project is scaffolded with the primary
navigation and locked visual direction:

- cover-led Home and Releases screens
- Home catalog totals
- release filters for albums, EPs, and singles
- alphabetical Tracks screen with an Available filter
- Search and Settings entry points
- dark, warm TrackLog theme

The app now connects to a user-selected TrackLog folder through Android's system
folder picker. It validates and safely imports `catalog.db` into private app
storage, queries the private copy read-only, and displays real totals, releases,
tracks, memberships, statuses, and available cover artwork. No sample catalogue
content is built into the app.

## Platform

- Minimum: Android 13 (API 33)
- Compile/target: Android API 37
- JDK 17
- Android Gradle Plugin 9.1.1
- Gradle 9.3.1
- Jetpack Compose BOM 2026.08.00

## Open the project

Open the repository root in Android Studio. Let Android Studio install the API 37
SDK and sync the project, then run the `app` configuration on an Android 13 or
newer device.

The source `catalog.db` is intentionally excluded from Git and must never be added
to the repository.

## Documentation

- [High-level design](docs/HLD.md)
- [Catalogue schema contract](docs/CATALOG_SCHEMA.md)
- [Version 1 UI design](docs/UX_DESIGN.md)
- [Android storage design](docs/STORAGE_DESIGN.md)
