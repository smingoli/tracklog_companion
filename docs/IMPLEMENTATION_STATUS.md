# TrackLog Companion — Implementation Status

Last updated: 25 August 2026

Baseline commit: `8f866dd`

This record distinguishes implemented and tested behaviour from the complete
Version 1 design contract in [UX_DESIGN.md](UX_DESIGN.md).

## Implemented

- Native Kotlin and Jetpack Compose Android application.
- Android 13 / API 33 minimum; API 37 compile and target SDK.
- Home, Releases, and Tracks primary navigation.
- Real catalogue totals and available-track calculation.
- Cover-led release grid with All, Albums, EPs, and Singles filters.
- Alphabetical track list with All and Available filters.
- Release details with artwork, metadata, optional description, and ordered tracks.
- Track details with conditional Overview, Lyrics, and Notes sections.
- Navigation between releases, tracks, and linked release membership.
- UTF-8 lyrics export through Android's document-creation flow.
- Live, case-insensitive global search grouped into Releases and Tracks.
- First-run TrackLog folder selection through the Storage Access Framework.
- Persisted read-only folder permission.
- Safe import into app-private storage using a pending candidate and atomic promotion.
- SQLite integrity, required-table, required-column, and schema-version validation.
- Explicit read-only SQLite access to the private working database.
- Windows-prefixed and relative artwork-path resolution with traversal rejection.
- Real cover loading with a non-blocking missing-artwork placeholder.
- Settings screen with folder change and manual catalogue refresh.
- Loading, folder-required, catalogue-error, empty-list, and no-search-results states.
- Successful builds and manual testing on an Android emulator and a Poco X6 Pro 5G.

## Next verification and polish

- Exercise and refine lost-folder-permission recovery.
- Exercise incompatible, corrupt, and missing database states with controlled fixtures.
- Confirm last-known-good catalogue behaviour for every refresh failure mode.
- Test missing and unreadable artwork at catalogue scale.
- Review accessibility with larger text and screen-reader navigation.
- Add automated repository, storage, and Compose UI tests.
- Refine spacing, typography, and system-back behaviour from real-device findings.

## Repository safety

The real `catalog.db`, copied catalogue data, artwork, signing keys, and generated
build output are excluded from source control. The desktop TrackLog catalogue
remains the canonical source and is never modified by the Android application.
