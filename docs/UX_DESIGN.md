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

## 9. Remaining screen-level design

The following screens belong to the approved navigation model but still require detailed layouts before implementation is considered visually complete:

- Tracks list, including the Available tracks filtered state.
- Global search and grouped search results.
- Settings and manual catalogue refresh.
- First-run TrackLog folder selection and validation.
- Loading, empty, missing-artwork, lost-permission, and incompatible-database states.

Their design must remain consistent with the principles and navigation contract in this document.
