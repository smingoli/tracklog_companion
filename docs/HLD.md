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

The exact screen set and navigation model will be defined during UX design after the current desktop schema has been reviewed.

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

The database component opens and queries `catalog.db` without modifying it. Before normal use, it performs lightweight compatibility checks, including the presence of required tables or an agreed schema-version marker.

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
3. The app closes any existing database connection.
4. The replacement file is validated and reopened read-only.
5. Cached catalogue results and artwork affected by the change are invalidated.

Version 1 will not replace the catalogue file itself. This avoids partial-copy and file-ownership complexity while still giving the user an explicit reload operation.

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

These are architectural preferences, not irreversible decisions. The database access choice in particular must be validated against opening a user-selected SQLite document through Android's Storage Access Framework.

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

1. Confirm the current `catalog.db` schema, relationships, expected catalogue size, and schema-version support.
2. Decide the exact Version 1 information architecture: primary tabs, browsing hierarchy, and track/release relationships.
3. Decide whether the app queries the selected database directly or first creates a private read-only working copy. Android document-provider and SQLite behaviour will influence this choice.
4. Define what constitutes a valid catalogue and how compatibility errors are communicated.
5. Confirm the minimum supported Android version and target phone or tablet form factors.
6. Define refresh behaviour when the database is replaced while the app is open.
7. Confirm whether search is global or separated by entity type for Version 1.

## 15. Architectural decisions already made

- The desktop TrackLog application is the canonical catalogue owner.
- Android catalogue access is read-only.
- The Android app operates on a copied database, not the desktop application's live file.
- Version 1 is offline and has no backend or account system.
- Artwork is stored beside the copied catalogue under the TrackLog folder.
- The app uses Android's system folder picker and retained folder permission.
- Existing Windows artwork paths are translated at runtime; future relative paths are also accepted.
