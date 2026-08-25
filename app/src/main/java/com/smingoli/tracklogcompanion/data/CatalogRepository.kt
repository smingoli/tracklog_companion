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

            val releases = database.rawQuery(
                """
                SELECT r.id, r.title, r.type, r.status, r.image_path, COUNT(rt.id)
                FROM releases r
                LEFT JOIN release_tracks rt ON rt.release_id = r.id
                GROUP BY r.id, r.title, r.type, r.status, r.image_path
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
                                trackCount = cursor.getInt(5),
                            ),
                        )
                    }
                }
            }

            val memberships = mutableMapOf<Long, MutableList<String>>()
            database.rawQuery(
                """
                SELECT rt.track_id, r.title
                FROM release_tracks rt
                JOIN releases r ON r.id = rt.release_id
                ORDER BY r.title COLLATE NOCASE
                """.trimIndent(),
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    memberships.getOrPut(cursor.getLong(0)) { mutableListOf() }.add(cursor.getString(1))
                }
            }

            val tracks = database.rawQuery(
                "SELECT id, title FROM tracks ORDER BY title COLLATE NOCASE",
                null,
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        add(CatalogTrack(id, cursor.getString(1), memberships[id].orEmpty()))
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
