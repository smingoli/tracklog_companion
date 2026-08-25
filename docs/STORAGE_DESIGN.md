# TrackLog Companion — Android Storage Design

Status: Approved

Decision date: 25 August 2026

Platform baseline: Android 13 / API 33

## 1. Decision

TrackLog Companion uses two storage surfaces for two different purposes:

1. The user-selected TrackLog folder is the external source of truth. Android's Storage Access Framework grants retained access to it.
2. A validated copy of `catalog.db` is stored in the app's private persistent storage and opened read-only by SQLite.

Release artwork remains in the selected TrackLog folder and is read through document-provider URIs. The app does not duplicate the complete artwork collection into private storage.

## 2. Rationale

Android's directory picker returns a tree URI and retained document-provider permission, not a conventional filesystem directory the app may freely address by path. `SQLiteDatabase.openDatabase` accepts a filesystem path or `File` and is designed to operate on a concrete database file.

Keeping the SQLite working copy in app-private internal storage provides:

- A reliable, seekable local file for SQLite.
- Independence from document-provider implementation details.
- Predictable database connection and replacement behaviour.
- Protection from the source file being replaced while a query is running.
- No need for broad storage permission.
- The ability to validate a candidate before replacing a known-good working copy.

The source `catalog.db` remains untouched and canonical.

## 3. Selected-folder access

First run launches `ACTION_OPEN_DOCUMENT_TREE`. After the user chooses the TrackLog folder, the app takes the offered persistable read permission and stores the tree URI in private preferences.

The requested permission is read-only. The app does not request `MANAGE_EXTERNAL_STORAGE`, legacy broad-storage access, or permission to write into the TrackLog source folder.

The tree is expected to contain:

```text
TrackLog/
├── catalog.db
└── images/
    └── releases/
        └── ...
```

If the persisted grant is lost or the provider becomes unavailable, the UI enters the approved Folder access needed flow.

## 4. Private working files

The app maintains these logical files beneath its persistent internal `filesDir`:

```text
catalogue/
├── catalog.db
└── catalog.pending.db
```

`catalog.db` is the last successfully imported working copy. `catalog.pending.db` exists only during import or refresh and is removed after completion or failure.

The working copy is app data, so Android removes it when the app is uninstalled. The user-owned TrackLog folder is unaffected.

## 5. Initial import and refresh

Initial import and manual refresh use the same transaction-like sequence:

1. Verify retained read access to the selected tree URI.
2. Locate `catalog.db` beneath that tree.
3. Remove any stale pending file from an interrupted earlier attempt.
4. Stream the source document into `catalog.pending.db` in app-private storage.
5. Flush and close the pending file.
6. Open the pending database with `SQLiteDatabase.OPEN_READONLY`.
7. Run the compatibility and integrity checks.
8. Close the pending database.
9. Atomically write the validated bytes to the active private `catalog.db`, preserving the old active file if promotion fails.
10. Remove the pending file.
11. Close the old application database connection, open the promoted active file read-only, and replace in-memory repository state.
12. Invalidate query and artwork caches affected by the refresh.

The active working copy is never overwritten before the candidate has passed validation. If reading, validation, or promotion fails, the app continues using the last known-good active copy when one exists.

Android's `AtomicFile` or an equivalent same-directory atomic promotion mechanism should be used for the final private-file replacement.

## 6. Candidate validation

Before promotion, the pending copy must pass:

- A readable SQLite-header/open check.
- SQLite `quick_check` or an equivalent lightweight integrity check.
- Presence of `schema_migrations`, `releases`, `tracks`, and `release_tracks`.
- Presence of all columns required by the supported repository queries.
- A latest migration version supported by the app.
- Basic relationship queries without unrecoverable SQLite errors.

Schema version 2 is the initial supported contract. A higher or otherwise incompatible version is rejected without modification.

An empty but structurally valid database is accepted and produces the approved empty-catalogue UI.

## 7. SQLite access

The active private database is opened explicitly with `SQLiteDatabase.OPEN_READONLY`. The connection is owned by the database component and shared through the catalogue repository rather than opened independently by screens.

The app does not:

- Create tables or indexes.
- Run desktop migrations.
- Store preferences or UI state in `catalog.db`.
- Enable write-ahead logging.
- Write corrections back to the source or working copy.

Every refresh has a defined connection handover so no query continues against a file being replaced.

## 8. Artwork access

Artwork paths are translated into safe paths relative to the selected TrackLog tree. The resolver accepts the current Windows-prefixed form and future relative paths, as defined in the catalogue schema contract.

After path normalisation and traversal checks, the storage layer walks the tree to the required document and returns its content URI. The image-loading layer reads that URI through `ContentResolver` and may maintain a bounded app cache.

Missing or inaccessible artwork returns a normal missing-artwork result rather than a catalogue failure. The UI uses the standard cover placeholder.

An in-memory or private cached index from normalised relative paths to document URIs may be used to avoid repeatedly walking the same directory tree. It is invalidated when the selected folder changes or the catalogue is refreshed.

## 9. Lyrics export

Lyrics export is separate from catalogue-folder access. The app launches Android's `ACTION_CREATE_DOCUMENT` flow with MIME type `text/plain` and a filename derived safely from the track title.

The selected output URI receives UTF-8 lyrics exactly as stored in the catalogue. This one-document grant requires no broad storage permission and does not give the app general write access to the destination folder.

## 10. Failure guarantees

- Source files are never modified.
- An invalid candidate never replaces a known-good working catalogue.
- Interrupted import leaves either the old working copy or the fully promoted new copy, not a partial active database.
- Lost tree permission does not expose internal paths or cause repeated permission dialogs.
- Artwork failures never block database browsing.
- The user sees the approved actionable state for permission, compatibility, empty data, or refresh failure.

## 11. Implementation boundary

The storage layer exposes operations equivalent to:

- Select and persist a TrackLog tree.
- Report whether retained tree access is valid.
- Import or refresh the private working catalogue.
- Open the active working catalogue read-only.
- Resolve and open artwork by safe relative path.
- Export lyrics to a user-created text document.

UI and repository code do not manipulate tree URIs, private files, or document streams directly.
