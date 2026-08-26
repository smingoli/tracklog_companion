# TrackLog Companion — User Guide

Applies to application version `0.3.0`.

## Requirements

- Android 13 or newer.
- A TrackLog desktop catalogue using schema version 2.
- Either an existing Android TrackLog folder or a desktop-generated companion ZIP.

## Option 1: Connect an existing folder

Prepare this structure on the Android device:

```text
TrackLog/
├── catalog.db
└── images/
    └── releases/
        └── ... cover files ...
```

On the first-run screen, choose **Select TrackLog folder**, select the `TrackLog`
folder itself, choose **Use this folder**, and approve access.

## Option 2: Import a TrackLog ZIP

The desktop export has this ZIP structure:

```text
catalog.db
images/
└── releases/
    └── ... cover files ...
```

1. Choose **Import TrackLog ZIP** during first-run setup, or open
   **Settings → Import TrackLog ZIP**.
2. Select the ZIP created by TrackLog Desktop. Its filename may vary.
3. Select or create the Android destination folder, such as
   `Documents/TrackLog`.
4. Choose **Use this folder** and approve access.
5. Wait while the app validates the archive and catalogue, extracts the files,
   and opens Home.

The destination should be the `TrackLog` folder itself. Do not select an existing
`catalog.db` file as the destination.

## Refresh or replace a catalogue

- Use **Settings → Refresh catalogue** after externally replacing `catalog.db`.
- Use **Settings → Import TrackLog ZIP** to import a new desktop export.
- Use **Settings → Change folder** to connect a different existing folder.

The current private working database is replaced only after a candidate passes
integrity and schema validation.

## Browsing

- **Home** shows total tracks, available tracks, and releases.
- **Releases** provides cover-led Album, EP, and Single filtering.
- **Tracks** lists all tracks or only tracks not assigned to a release.
- **Search** matches release and track titles and groups the results.
- Release and track details expose descriptions, membership, lyrics, and notes
  only when those fields are present.
- Lyrics can be exported as UTF-8 plain text through Android's save-file dialog.

## Landscape and tablets

- Rotate the device normally; the selected destination, filters, selected item,
  search query, and track-detail section are retained.
- Compact screens use bottom navigation. Wider landscape and tablet screens use
  a navigation rail at the left edge.
- The Releases grid automatically adds columns when more width is available.
- Release Detail uses a split layout on wider screens, keeping cover artwork and
  metadata beside the independently scrollable track list.

## Privacy and storage

- Catalogue browsing is read-only.
- ZIP extraction writes only to the destination folder explicitly approved by
  the user.
- The app requests no broad storage permission and requires no network access.
- Catalogue processing stays on the Android device.
