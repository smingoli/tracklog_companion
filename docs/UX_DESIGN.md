# TrackLog Companion — Version 1 UI Design

Status: Approved baseline  
Decision date: 25 August 2026

This document is the implementation contract for the Version 1 user-interface decisions approved during design. Changes to an approved decision should be made deliberately and recorded here rather than emerging incidentally during implementation.

## 1. Experience principles

- The interface is phone-first, native Android, and read-only.
- Catalogue content is more important than administrative metadata.
- Release artwork is the primary visual anchor.
- Primary destinations remain consistently reachable through bottom navigation.
- Empty fields do not produce empty headings, cards, or tabs.
- Common actions are visibly labelled rather than hidden in overflow menus.
- The interface must remain usable with Android text scaling and accessibility services.

## 2. Application navigation

The persistent bottom navigation contains three primary destinations:

1. **Home**
2. **Releases**
3. **Tracks**

The active destination is visually indicated. Detail screens preserve the originating destination: for example, a track opened from a release keeps Releases active, while a track opened from the Tracks destination keeps Tracks active.

Global actions appear in the top application bar:

- **Search** searches across the catalogue.
- **Settings** owns the TrackLog folder selection and catalogue refresh controls.

Detail screens use a top-left back action and a concise screen label.

## 3. Home

Home presents three prominent catalogue totals:

| Total | Meaning | Tap destination |
| --- | --- | --- |
| Total tracks | Every track in the catalogue | Complete Tracks list |
| Available tracks | Tracks not assigned to a release | Tracks list filtered to unassigned tracks |
| Total releases | Albums, EPs, and singles | Releases screen |

**Available tracks** includes supporting text such as **Not assigned to a release** so its meaning is immediately clear.

Home also provides Search and Settings in the top app bar. It does not duplicate full release or track lists.

## 4. Releases

Releases uses a two-column, cover-led grid.

The local filters are:

- **All**
- **Albums**
- **EPs**
- **Singles**

Each release tile contains:

- Cover artwork, or a standard placeholder if artwork is unavailable.
- Release title.
- Release type.
- Track count.
- Release status.

Tapping any part of the tile opens Release Detail. Search remains available in the top app bar.

## 5. Release Detail

Release Detail contains, in order:

1. Large cover artwork.
2. Release title.
3. Release type and track count.
4. Release status.
5. Release description, only when present.
6. The ordered track list.

Track rows contain only:

- Track order.
- Track title.
- A navigation indicator.

Track status, BPM, and musical key are deliberately omitted. Tapping a track opens Track Detail.

## 6. Track Detail

Track Detail prioritises readable catalogue content rather than production metadata.

The header contains:

- Track title.
- The number of releases on which the track appears, when at least one exists.

The content uses tabs:

- **Overview**
- **Lyrics**
- **Notes**, only when notes are present.

Track status, BPM, and musical key are not displayed anywhere on Track Detail.

### Overview

Overview contains:

- **About**, showing the track description when present.
- **Releases**, showing every release on which the track appears.

Each linked release row contains a cover thumbnail, title, type, and track position. Tapping it opens Release Detail.

If the track has neither a description nor a linked release, the Overview tab is omitted rather than shown empty.

### Lyrics

Lyrics are presented as readable, preformatted text that preserves headings, line breaks, and stanza spacing from the catalogue.

A visibly labelled **Export** action appears at the top of the Lyrics tab. Export invokes Android's system document-creation flow and suggests:

```text
<track title>.txt
```

The user chooses the filename and destination. The app writes UTF-8 plain text and does not require broad filesystem permission.

The exported file contains the lyrics exactly as stored in the catalogue. No status, BPM, key, or internal database metadata is added.

### Notes

Notes use the same readable text treatment as the other content. The Notes tab exists only when the track has non-empty notes.

## 7. Conditional content

The UI omits absent optional content rather than displaying empty placeholders:

- No release description: go directly from the release header to Tracks.
- No track description: omit About.
- No linked releases: omit Releases from Overview.
- No lyrics: omit the Lyrics tab and export action.
- No notes: omit the Notes tab.
- Only one remaining track-detail section: display it directly without a one-option tab bar.

Missing release artwork is the exception: a consistent artwork placeholder preserves the layout.

## 8. Accessibility and interaction requirements

- Every icon-only action has an accessible label.
- Release tiles and list rows are full-size touch targets, not small text links.
- Selected tabs, filters, and bottom-navigation destinations are communicated without relying on colour alone.
- Artwork has useful descriptions where possible and is marked decorative when the surrounding text already provides the same identity.
- Lyrics and descriptions support selection and Android text scaling.
- Screen-reader traversal follows the visual reading order.
- Loading, empty, and error states use clear text and provide an actionable next step.

## 9. Tracks

Tracks uses a compact alphabetical list rather than artwork tiles.

The local filters are:

- **All**, showing every track.
- **Available**, showing only tracks not assigned to a release.

Each track row contains:

- Track title.
- Release membership as supporting text.
- A navigation indicator.

An unassigned track displays **Not assigned to a release** in place of release membership. Status, BPM, and musical key are not shown. Tapping a row opens Track Detail and keeps Tracks active in the bottom navigation.

Alphabetical section labels help scanning. The result total updates to reflect the selected filter. Search remains available in the top app bar.

The Available-tracks total on Home opens this screen with Available already selected.

## 10. Global Search

Global Search is a focused temporary screen opened from the top app bar. It uses:

- A top-left back action that returns to the originating screen.
- One prominent search field with a clear action.
- Live results grouped under **Releases** and **Tracks**.

Search initially matches release and track titles case-insensitively. Empty result groups are omitted. Results are never mixed into an ambiguous flat list.

Release results contain:

- Cover thumbnail or artwork placeholder.
- Release title.
- Release type and track count.

Track results contain:

- Track title.
- Release membership, or **Not assigned to a release**.

Selecting a result opens its normal Release Detail or Track Detail screen. The opened detail preserves the primary navigation destination from which Search was launched.

The screen has explicit initial and no-results states. Bottom navigation is hidden while Search is active so the search task remains focused; the back action restores the previous screen and query context does not become a new primary destination.

## 11. Settings and Catalogue Refresh

Settings is a focused utility screen opened from the top app bar. It is not a bottom-navigation destination.

The screen contains three concise sections:

### TrackLog folder

The selected folder is displayed using its friendly name and Android document path. A visibly labelled **Change folder** action opens Android's system folder picker.

Changing the folder does not discard the current working catalogue until the replacement selection has passed validation. Cancelling the picker leaves the current folder unchanged.

### Catalogue

The catalogue section displays:

- The `catalog.db` filename.
- A clear health state such as **Ready**.
- Current release and track totals.
- The time of the last successful refresh.
- A prominent **Refresh catalogue** action.

Refresh closes the current database connection, validates the selected catalogue, reopens it read-only, invalidates affected cached data, and updates the displayed totals. The action shows progress while refresh is running and an explicit success or failure result when it completes.

If refresh fails, the last successfully opened catalogue remains available whenever technically possible. Failure must not silently replace a working catalogue with an invalid one.

### About

About identifies TrackLog Companion and confirms that database access is read-only. It remains informational and contains no unnecessary preferences.

## 12. First Run and Folder Selection

First run uses one calm, single-purpose screen rather than a multi-step wizard.

The screen:

- Welcomes the user with **Connect your catalogue**.
- Explains that the app needs the copied Android TrackLog folder.
- States that access is remembered and the catalogue is opened read-only.
- Shows the expected `catalog.db` and `images/releases` structure.
- Provides one primary **Select TrackLog folder** action.
- States that catalogue data remains on the device.

The primary action opens Android's system folder picker. After a selection, the app immediately validates:

1. Retained access can be granted to the selected folder.
2. `catalog.db` exists and is readable.
3. The database is valid and uses a supported schema version.
4. The artwork directory can be accessed when present.

Validation progress is shown on the same screen. A valid folder produces a clear ready state with release and track totals, followed by **Continue to TrackLog**. Continuing opens Home.

The artwork directory is desirable but missing artwork must not prevent catalogue browsing. A valid readable database is the essential requirement.

An invalid selection remains on the same screen, explains the specific problem, and offers **Choose another folder**. The user is never sent through a generic error dialog or a second setup wizard.

## 13. Remaining screen-level design

The following screens belong to the approved navigation model but still require detailed layouts before implementation is considered visually complete:

- Loading, empty, missing-artwork, lost-permission, and incompatible-database states.

Their design must remain consistent with the principles and navigation contract in this document.
