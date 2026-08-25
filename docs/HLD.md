# TrackLog Companion — High-Level Design

Status: Initial draft  
Target: Version 1

## 1. Purpose

TrackLog Companion is a private Android application for browsing a TrackLog music catalogue away from the desktop application.

The Android app uses a copy of the desktop application's SQLite catalogue and associated release artwork. It is a read-only companion: the desktop application remains the only system that creates or changes catalogue data.

## 2. Design goals

- Present the TrackLog catalogue in a phone-friendly native Android interface.
- Read the existing `catalog.db` without introducing a second data format.
- Keep the Android copy strictly read-only.
- Work fully offline, without a server, cloud service, or user account.
- Support both existing Windows-style artwork paths and future portable relative paths.
- Keep catalogue replacement simple and safe.
- Avoid broad Android filesystem permissions.

## 3. Version 1 scope

Version 1 is expected to provide:

- Initial selection of the Android `TrackLog` data folder.
- Validation that a usable catalogue exists in the selected folder.
- Browsing of releases and tracks.
- Release and track detail views.
- Display of release artwork.
- Display of lyrics, descriptions, credits, and other catalogue fields where present.
- Catalogue search.
- A manual refresh/reload action after the files have been replaced.
- Clear handling of missing artwork, unsupported database versions, and invalid files.

The approved screen and navigation decisions are documented in the [Version 1 UI design](UX_DESIGN.md). The current desktop schema is documented in the [catalogue schema contract](CATALOG_SCHEMA.md).

## 4. Out of scope for Version 1

- Creating, editing, or deleting catalogue data.
- Writing to `catalog.db` for app state, preferences, or migrations.
- Synchronising directly with the desktop application.
- Network shares or simultaneous access to the desktop's live database.
- A backend service, cloud database, or TrackLog account.
- Public distribution through Google Play.
- Audio playback, unless added as a separate future requirement.

## 5. System context

```text
┌───────────────────────────────┐
│ TrackLog desktop application  │
│                               │
│ Canonical catalogue owner     │
│ Reads and writes catalogue    │
└───────────────┬───────────────┘
                │
                │ User copies or exports files
                ▼
┌───────────────────────────────┐
│ Android: Documents/TrackLog   │
│                               │
│ catalog.db                    │
│ images/releases/...           │
└───────────────┬───────────────┘
                │
                │ Read-only access
                ▼
┌───────────────────────────────┐
│ TrackLog Companion            │
│                               │
│ Browses, searches, displays   │
└───────────────────────────────┘
```

The file-transfer method is deliberately outside the application boundary in Version 1. Files may be copied by USB, a file-management tool, or another user-controlled mechanism.

## 6. On-device data contract

The selected TrackLog folder has this logical structure:

```text
TrackLog/
├── catalog.db
└── images/
    └── releases/
        └── ... artwork files ...
```

On first use, Android's system folder picker asks the user to select the `TrackLog` folder. The app retains permission to access that folder through Android's Storage Access Framework. The app does not request unrestricted access to device storage.

The catalogue file is opened in read-only mode. Application preferences, such as the selected folder reference or UI settings, are stored separately in the app's private storage and never in `catalog.db`.

## 7. Major application components

### User interface

Native Android screens provide catalogue browsing, search, release details, track details, loading states, and actionable error messages.

### Presentation and navigation

Presentation logic converts catalogue data into screen state and coordinates navigation. It remains independent of SQLite and Android document-provider details.

### Catalogue repository

The repository exposes read-only catalogue operations to the rest of the app. It is the boundary between application features and the physical SQLite schema.

This layer will contain explicit queries rather than allowing UI code to access database tables directly. That isolation limits the impact of future schema changes.

### Database access

The database component opens and queries an app-private working copy of `catalog.db` without modifying it. Before promotion to the working copy, an imported candidate is checked for SQLite integrity, required tables, and the latest entry in `schema_migrations`. Version 1 will initially support desktop schema version 2; future schema versions must be tested before being accepted.

### TrackLog folder access

The storage component owns the persisted Android folder permission, finds `catalog.db`, opens documents, and reports missing or inaccessible files.

### Artwork path resolver

The path resolver converts catalogue artwork references into paths relative to the selected TrackLog folder. It supports:

1. Preferred portable paths, such as `images/releases/cover.jpg`.
2. Existing desktop paths, such as `%LocalAppData%\TrackLog\data\images\releases\cover.jpg`.

The resolver normalises separators, removes the known desktop prefix when present, and rejects paths that escape the selected TrackLog folder.

## 8. Primary data flows

### First launch

1. The app explains which folder is required.
2. The user selects the Android `TrackLog` folder using the system picker.
3. The app retains access to the selected folder.
4. The app locates and validates `catalog.db`.
5. The catalogue home screen opens, or a useful validation error is shown.

### Browse or search

1. A screen requests data through the catalogue repository.
2. The repository runs a read-only query against SQLite.
3. Results are mapped from database rows into application models.
4. The presentation layer produces screen state for the UI.

### Load artwork

1. A catalogue record supplies an artwork path.
2. The path resolver converts it to a safe folder-relative path.
3. The storage component opens the image through the selected folder permission.
4. The UI displays the image or a standard placeholder.

### Refresh catalogue

1. The user replaces `catalog.db` and, if needed, artwork in the selected folder.
2. The user invokes refresh, or returns to the app after replacement.
3. The app copies the selected source database into a private staging file.
4. The staging file is validated before atomically replacing the private working copy.
5. The app closes the existing database connection and opens the promoted working copy read-only.
6. Cached catalogue results and artwork affected by the change are invalidated.

Version 1 never writes to or replaces the user-owned source catalogue. A failed import leaves the previous private working copy intact.

## 9. Failure handling

The app must fail safely and explain what the user can do next.

- Missing database: ask the user to copy `catalog.db` or select the correct folder.
- Lost folder permission: ask the user to select the folder again.
- Invalid or incompatible database: do not open it; report the compatibility problem.
- Missing or unreadable artwork: show a placeholder without blocking catalogue use.
- Database replaced during use: close and reopen it through the refresh flow.
- Query or data error: preserve navigation where practical and show a recoverable error state.

## 10. Security and privacy

- All catalogue processing occurs on the device.
- The app requires no internet permission for Version 1.
- No analytics, advertising, telemetry, or account data is collected.
- The database is never opened with write access.
- Folder access is limited to the directory explicitly selected by the user.
- Artwork paths are treated as untrusted catalogue data and cannot traverse outside the selected folder.

## 11. Quality attributes

### Compatibility

The database compatibility contract must be explicit. A schema-version mechanism is preferred so the app can reject unsupported catalogues cleanly rather than failing unpredictably.

### Performance

Catalogue lists and searches should remain responsive for the expected catalogue size. Queries will be paged or limited where appropriate, and image loading will use bounded memory and caching.

### Reliability

Read-only access protects the canonical catalogue format. Database connections must have a clear lifecycle so a copied replacement can be reopened reliably.

### Maintainability

UI, application models, SQLite queries, storage access, and path translation remain separate responsibilities. Desktop schema assumptions should be confined to the database and mapping layers.

### Accessibility

Screens should support Android text scaling, useful content descriptions, sufficient contrast, and touch targets appropriate for a phone interface.

## 12. Proposed technology direction

The expected implementation direction is:

- Kotlin.
- Jetpack Compose for the user interface.
- Android Navigation for screen navigation.
- Android Storage Access Framework for folder access.
- A lightweight SQLite access layer opened explicitly as read-only.
- Kotlin coroutines for database and document operations off the main thread.
- Android 13 / API 33 as the minimum supported platform.
- API 37 for compilation and target behaviour.

The storage implementation is defined in the [Android storage design](STORAGE_DESIGN.md). Framework and library versions may advance during development, but changing the minimum supported platform is a product decision and must be recorded explicitly.

## 13. Evolution path

Possible later additions include:

- A desktop export command that prepares the complete Android folder.
- Safer in-app import using a staged copy and atomic replacement.
- Automated transfer or synchronisation.
- Additional filtering and catalogue views.
- Tablet layouts.
- Portable relative artwork paths written directly by the desktop application.

None of these require a backend to be introduced into Version 1.

## 14. Decisions required before detailed design

The initial application targets phones. Tablet-specific layouts are a possible later enhancement rather than a Version 1 requirement.

## 15. Architectural decisions already made

- The desktop TrackLog application is the canonical catalogue owner.
- Android catalogue access is read-only.
- The Android app operates on a copied database, not the desktop application's live file.
- Version 1 is offline and has no backend or account system.
- Artwork is stored beside the copied catalogue under the TrackLog folder.
- The app uses Android's system folder picker and retained folder permission.
- Existing Windows artwork paths are translated at runtime; future relative paths are also accepted.
- The initial Android compatibility target is catalogue schema version 2.
- Releases and tracks have a many-to-many relationship through ordered `release_tracks` rows.
- Tracks without a release are valid catalogue records and must remain discoverable.
- Primary navigation is a three-destination bottom bar: Home, Releases, and Tracks.
- Search is global and Settings owns folder selection and catalogue refresh.
- Release browsing is cover-led and release details lead to ordered track lists.
- Track details prioritise description, release membership, lyrics, and notes; status, BPM, and musical key are not displayed.
- Lyrics can be exported through Android's system save flow as a plain-text file.
- The selected folder is accessed through a persisted Storage Access Framework tree URI.
- `catalog.db` is copied through a validated staging file into app-private persistent storage and only then opened read-only.
- Artwork remains in the selected TrackLog folder and is opened through document-provider URIs.
- The minimum supported platform is Android 13 / API 33.
- The initial build compiles against and targets API 37.
