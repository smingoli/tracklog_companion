package com.smingoli.tracklogcompanion.data

import android.database.sqlite.SQLiteDatabase
import java.io.File

class CatalogRepository(private val databaseFile: File) {
    fun readSnapshot(treeUri: android.net.Uri): CatalogSnapshot =
        SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
            val totals = CatalogTotals(
                tracks = database.scalarInt("SELECT COUNT(*) FROM tracks"),
                availableTracks = database.scalarInt(
                    "SELECT COUNT(*) FROM tracks t WHERE NOT EXISTS " +
                        "(SELECT 1 FROM release_tracks rt WHERE rt.track_id = t.id)",
                ),
                releases = database.scalarInt("SELECT COUNT(*) FROM releases"),
            )

            val releaseTrackIds = mutableMapOf<Long, MutableList<Long>>()
            val memberships = mutableMapOf<Long, MutableList<TrackReleaseLink>>()
            database.rawQuery(
                """
                SELECT rt.release_id, rt.track_id, rt.track_order,
                       r.title, r.type, r.image_path
                FROM release_tracks rt
                JOIN releases r ON r.id = rt.release_id
                ORDER BY rt.release_id, rt.track_order
                """.trimIndent(),
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val releaseId = cursor.getLong(0)
                    val trackId = cursor.getLong(1)
                    releaseTrackIds.getOrPut(releaseId) { mutableListOf() }.add(trackId)
                    memberships.getOrPut(trackId) { mutableListOf() }.add(
                        TrackReleaseLink(
                            releaseId = releaseId,
                            title = cursor.getString(3),
                            type = cursor.getString(4),
                            imagePath = cursor.getString(5),
                            trackOrder = cursor.getInt(2),
                        ),
                    )
                }
            }

            val releases = database.rawQuery(
                """
                SELECT r.id, r.title, r.type, r.status, r.image_path, r.description, COUNT(rt.id)
                FROM releases r
                LEFT JOIN release_tracks rt ON rt.release_id = r.id
                GROUP BY r.id, r.title, r.type, r.status, r.image_path, r.description
                ORDER BY r.title COLLATE NOCASE
                """.trimIndent(),
                null,
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            CatalogRelease(
                                id = cursor.getLong(0),
                                title = cursor.getString(1),
                                type = cursor.getString(2),
                                status = cursor.getString(3),
                                imagePath = cursor.getString(4),
                                description = cursor.getString(5),
                                trackCount = cursor.getInt(6),
                                trackIds = releaseTrackIds[cursor.getLong(0)].orEmpty(),
                            ),
                        )
                    }
                }
            }

            val tracks = database.rawQuery(
                "SELECT id, title, description, lyrics, notes FROM tracks ORDER BY title COLLATE NOCASE",
                null,
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        add(
                            CatalogTrack(
                                id = id,
                                title = cursor.getString(1),
                                description = cursor.getString(2),
                                lyrics = cursor.getString(3),
                                notes = cursor.getString(4),
                                releases = memberships[id].orEmpty(),
                            ),
                        )
                    }
                }
            }

            CatalogSnapshot(totals, releases, tracks, treeUri)
        }

    private fun SQLiteDatabase.scalarInt(sql: String): Int =
        rawQuery(sql, null).use { cursor ->
            check(cursor.moveToFirst()) { "Catalogue query returned no result" }
            cursor.getInt(0)
        }
}
