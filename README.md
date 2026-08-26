# TrackLog Companion

Read-only Android companion app for browsing a TrackLog music catalog.

Current application version: **0.3.1** (`versionCode 5`). The source repository
is public; TrackLog catalogue content and generated APKs are not part of it.

## Current status

The native Kotlin and Jetpack Compose application currently provides:

- cover-led Home and Releases screens
- Home catalog totals
- release filters for albums, EPs, and singles
- alphabetical Tracks screen with an Available filter
- release and track detail navigation
- conditional Overview, Lyrics, and Notes content
- UTF-8 plain-text lyrics export
- grouped global search across releases and tracks
- folder selection, catalogue validation, and manual refresh
- secure desktop-export ZIP import into a user-selected destination folder
- responsive portrait, landscape, and tablet layouts
- navigation rail on wider screens and adaptive release grids
- split release-detail layout in landscape and at tablet widths
- rotation-safe destination, filter, search, and detail-section state
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

## Connect a catalogue

The app supports two setup paths:

1. Select an existing Android folder containing `catalog.db` and
   `images/releases/`.
2. Select **Import TrackLog ZIP**, choose a desktop companion-export ZIP, then
   choose the Android destination folder where it should be extracted.

Both paths validate the database before it becomes the app's active read-only
working catalogue.

## Documentation

- [High-level design](docs/HLD.md)
- [Catalogue schema contract](docs/CATALOG_SCHEMA.md)
- [Version 1 UI design](docs/UX_DESIGN.md)
- [Android storage design](docs/STORAGE_DESIGN.md)
- [Implementation status](docs/IMPLEMENTATION_STATUS.md)
- [User guide](docs/USER_GUIDE.md)
