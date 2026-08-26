# TrackLog Companion — Implementation Status

Last updated: 26 August 2026

Application version: `0.3.1` (`versionCode 5`)

Feature baseline: the `0.3.1` adaptive-layout refinement release.

This record distinguishes implemented and tested behaviour from the complete
Version 1 design contract in [UX_DESIGN.md](UX_DESIGN.md).

## Implemented

- Native Kotlin and Jetpack Compose Android application.
- Android 13 / API 33 minimum; API 37 compile and target SDK.
- Home, Releases, and Tracks primary navigation.
- Bottom navigation at compact widths and a navigation rail at wider widths.
- Scroll-safe setup, error, Home, Settings, track, and detail content in landscape.
- Adaptive release grids that add columns as usable width increases.
- Split release-detail layout at landscape and tablet widths.
- Compact landscape release-detail artwork and single-line destination titles
  (`TrackLog - Releases` and `TrackLog - Tracks`) to preserve vertical space.
- Saved destination, filter, selected-item, search, and track-section UI state across rotation.
- Sampled cover-art decoding sized for its presentation to limit memory use on large screens.
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
- Secure desktop companion ZIP import from first-run setup and Settings, with a
  runtime-selected extraction destination.
- Correct conversion of Storage Access Framework tree URIs when creating files
  and directories in an empty ZIP-import destination.
- Loading, folder-required, catalogue-error, empty-list, and no-search-results states.
- Successful builds and manual testing on an Android emulator and a Poco X6 Pro 5G.
- Successful real-device ZIP import using a desktop-generated 70 MB companion
  export containing `catalog.db` and release artwork.
- Installable signed debug APKs produced for Samsung Galaxy testing.

## Next verification and polish

- Exercise and refine lost-folder-permission recovery.
- Exercise incompatible, corrupt, and missing database states with controlled fixtures.
- Confirm last-known-good catalogue behaviour for every refresh failure mode.
- Test missing and unreadable artwork at catalogue scale.
- Review accessibility with larger text and screen-reader navigation.
- Complete a dedicated landscape and tablet device test matrix for the new adaptive layouts.
- Add automated repository, storage, and Compose UI tests.
- Refine spacing, typography, and system-back behaviour from real-device findings.

## Repository safety

The real `catalog.db`, copied catalogue data, artwork, signing keys, and generated
build output are excluded from source control. The desktop TrackLog catalogue
remains the canonical source and is never modified by the Android application.

The GitHub repository is public. No catalogue content or generated test APK is
tracked in Git.
