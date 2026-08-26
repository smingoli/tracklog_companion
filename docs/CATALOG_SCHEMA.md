# Catalogue Schema Contract

Status: Reviewed and implemented
Source reviewed: TrackLog desktop `catalog.db`, 25 August 2026  
Supported schema version: 2 (`many_to_many_release_tracks`)
Implementation baseline: App `0.3.1`

This document describes the parts of the desktop TrackLog SQLite database on which the Android companion depends. It records structure and behaviour only; the source database and catalogue content are not stored in this repository.

## Compatibility check

Before opening the catalogue for normal use, the app should verify that:

1. The file is a readable SQLite database.
2. `schema_migrations`, `releases`, `tracks`, and `release_tracks` exist.
3. The highest migration version is one the app supports.
4. The required columns are present.

The initial Android release supports migration version 2. A database with a higher migration version should be treated as unsupported until its changes have been reviewed. This is safer than assuming an unknown desktop schema remains compatible.

`PRAGMA user_version` is currently `0`; compatibility therefore comes from the `schema_migrations` table rather than SQLite's user-version field.

## Entity model

```text
┌──────────────┐       ┌──────────────────┐       ┌──────────────┐
│ releases     │ 1   * │ release_tracks   │ *   1 │ tracks       │
├──────────────┤───────├──────────────────┤───────├──────────────┤
│ id           │       │ release_id       │       │ id           │
│ internal_code│       │ track_id         │       │ internal_code│
│ title        │       │ track_order      │       │ title        │
│ type         │       └──────────────────┘       │ ...          │
│ ...          │                                  └──────────────┘
└──────────────┘
```

A release contains zero or more ordered tracks. A track may appear on zero, one, or multiple releases. Consequently, release membership and track order are properties of `release_tracks`, not of `tracks`.

Tracks with no release are valid. The Android information architecture must provide a way to discover them rather than exposing tracks only by drilling into releases.

## Tables

### `schema_migrations`

| Column | Type | Rules |
| --- | --- | --- |
| `version` | INTEGER | Primary key |
| `name` | TEXT | Required |
| `applied_at` | TEXT | Required |

Observed migrations:

| Version | Name |
| --- | --- |
| 1 | `initial_schema` |
| 2 | `many_to_many_release_tracks` |

### `releases`

| Column | Type | Rules |
| --- | --- | --- |
| `id` | INTEGER | Primary key |
| `internal_code` | TEXT | Required, unique |
| `title` | TEXT | Required |
| `type` | TEXT | Required; `Album`, `EP`, or `Single` |
| `status` | TEXT | Required; `Planned`, `In Progress`, or `Released` |
| `description` | TEXT | Optional |
| `image_path` | TEXT | Optional |
| `created_at` | TEXT | Required |
| `updated_at` | TEXT | Required |

Indexes support lookup or sorting by title, type, status, and update time.

### `tracks`

| Column | Type | Rules |
| --- | --- | --- |
| `id` | INTEGER | Primary key |
| `internal_code` | TEXT | Required, unique |
| `title` | TEXT | Required |
| `status` | TEXT | Required; `Idea`, `Draft`, `In Progress`, or `Final` |
| `description` | TEXT | Optional |
| `lyrics` | TEXT | Optional |
| `notes` | TEXT | Optional |
| `bpm` | INTEGER | Optional; positive when present |
| `musical_key` | TEXT | Optional |
| `created_at` | TEXT | Required |
| `updated_at` | TEXT | Required |

Indexes support lookup or sorting by title, status, and update time.

### `release_tracks`

| Column | Type | Rules |
| --- | --- | --- |
| `id` | INTEGER | Primary key |
| `release_id` | INTEGER | Required; foreign key to `releases.id` |
| `track_id` | INTEGER | Required; foreign key to `tracks.id` |
| `track_order` | INTEGER | Required; positive |
| `created_at` | TEXT | Required |

The table enforces:

- One occurrence of a particular track per release.
- One track per position within a release.
- Cascade deletion when a referenced release or track is deleted by the desktop app.

Indexes support joins by release or track and ordered retrieval of a release's tracks.

## Artwork references

The reviewed catalogue stores release artwork using this form:

```text
%LocalAppData%\TrackLog\data\images\releases\<filename>
```

Both `.jpg` and `.png` files occur. The Android app must remove the known desktop prefix, normalise path separators, and resolve the remainder beneath the selected TrackLog folder:

```text
images/releases/<filename>
```

The resolver should also accept paths already stored in this relative form. It must reject absolute or parent-traversal results that could escape the selected folder.

## Read-only query model

The Android app should expose database results through application models rather than returning raw rows to the UI. The initial repository operations are expected to include:

- List and search releases, optionally filtered by type or status.
- Get one release and its tracks ordered by `track_order`.
- List and search all tracks, including unassigned tracks.
- Get one track and every release on which it appears.
- Retrieve the latest supported migration version.

All queries and connections must remain read-only. Foreign-key enforcement in the reviewed desktop connection state was disabled, so the Android reader should not assume that foreign-key declarations alone guarantee clean source data; joins should handle missing related rows safely.

## Reviewed sample characteristics

These figures describe the reviewed catalogue, not fixed product limits:

| Measure | Observed value |
| --- | ---: |
| Database size | 432 KiB |
| Releases | 12 |
| Tracks | 107 |
| Release–track links | 95 |
| Unassigned tracks | 14 |
| Tracks appearing on multiple releases | 2 |

SQLite's quick integrity check returned `ok`. The database uses UTF-8 encoding, 4 KiB pages, and delete-journal mode. No companion WAL file was present during inspection.

The app should not encode the observed row counts as limits. They only show that Version 1 does not require a large-catalogue architecture, while normal paging or bounded queries remain sensible.
