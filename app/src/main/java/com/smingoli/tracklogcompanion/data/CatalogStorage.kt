package com.smingoli.tracklogcompanion.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import android.util.AtomicFile
import java.io.File

class CatalogStorage(private val context: Context) {
    private val preferences = context.getSharedPreferences("catalog", Context.MODE_PRIVATE)
    private val catalogDirectory = File(context.filesDir, "catalogue")
    val activeDatabase = File(catalogDirectory, "catalog.db")
    private val pendingDatabase = File(catalogDirectory, "catalog.pending.db")

    fun savedTreeUri(): Uri? = preferences.getString(KEY_TREE_URI, null)?.let(Uri::parse)

    fun hasReadableTree(uri: Uri): Boolean = runCatching {
        val root = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        context.contentResolver.query(root, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
            ?.use { it.moveToFirst() } == true
    }.getOrDefault(false)

    fun import(uri: Uri): CatalogSnapshot {
        catalogDirectory.mkdirs()
        pendingDatabase.delete()

        val source = findChild(uri, "catalog.db")
            ?: error("This folder does not contain catalog.db")

        try {
            context.contentResolver.openInputStream(source)?.use { input ->
                pendingDatabase.outputStream().buffered().use { output -> input.copyTo(output) }
            } ?: error("catalog.db could not be opened")

            validate(pendingDatabase)
            promotePendingDatabase()

            preferences.edit().putString(KEY_TREE_URI, uri.toString()).apply()
            return CatalogRepository(activeDatabase).readSnapshot(uri)
        } finally {
            pendingDatabase.delete()
        }
    }

    fun openSaved(): CatalogSnapshot? {
        val uri = savedTreeUri() ?: return null
        if (!activeDatabase.isFile || !hasReadableTree(uri)) return null
        return CatalogRepository(activeDatabase).readSnapshot(uri)
    }

    fun findArtwork(treeUri: Uri, storedPath: String?): Uri? {
        val parts = normaliseArtworkPath(storedPath) ?: return null
        var current = treeUri
        for (part in parts) current = findChild(current, part) ?: return null
        return current
    }

    private fun findChild(parentUri: Uri, displayName: String): Uri? {
        val resolver = context.contentResolver
        val parentId = if (DocumentsContract.isDocumentUri(context, parentUri)) {
            DocumentsContract.getDocumentId(parentUri)
        } else {
            DocumentsContract.getTreeDocumentId(parentUri)
        }
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, parentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        return resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1).equals(displayName, ignoreCase = true)) {
                    return@use DocumentsContract.buildDocumentUriUsingTree(parentUri, cursor.getString(0))
                }
            }
            null
        }
    }

    private fun promotePendingDatabase() {
        val target = AtomicFile(activeDatabase)
        val output = target.startWrite()
        try {
            pendingDatabase.inputStream().buffered().use { input -> input.copyTo(output) }
            target.finishWrite(output)
        } catch (error: Throwable) {
            target.failWrite(output)
            throw error
        }
    }

    private fun normaliseArtworkPath(path: String?): List<String>? {
        if (path.isNullOrBlank()) return null
        val normalised = path.replace('\\', '/').substringAfter("/TrackLog/data/", path.replace('\\', '/'))
            .trimStart('/')
        val parts = normalised.split('/').filter(String::isNotBlank)
        if (parts.isEmpty() || parts.any { it == "." || it == ".." }) return null
        val imagesIndex = parts.indexOfFirst { it.equals("images", ignoreCase = true) }
        if (imagesIndex < 0) return null
        return parts.drop(imagesIndex)
    }

    private fun validate(file: File) {
        if (file.length() < 16) error("catalog.db is empty or unreadable")
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
            val integrity = database.rawQuery("PRAGMA quick_check", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            if (integrity != "ok") error("catalog.db failed its integrity check")

            val requiredTables = setOf("schema_migrations", "releases", "tracks", "release_tracks")
            val tables = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table'",
                null,
            ).use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) } }
            if (!tables.containsAll(requiredTables)) error("catalog.db is not a compatible TrackLog catalogue")

            val schemaVersion = database.rawQuery(
                "SELECT MAX(version) FROM schema_migrations",
                null,
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
            if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
                error("Catalogue schema $schemaVersion is not supported; this app supports version $SUPPORTED_SCHEMA_VERSION")
            }

            requireColumns(database, "releases", setOf("id", "title", "type", "status", "image_path"))
            requireColumns(database, "tracks", setOf("id", "title"))
            requireColumns(database, "release_tracks", setOf("id", "release_id", "track_id", "track_order"))
        }
    }

    private fun requireColumns(database: SQLiteDatabase, table: String, required: Set<String>) {
        val actual = database.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name"))) }
        }
        if (!actual.containsAll(required)) error("Catalogue table $table is missing required fields")
    }

    companion object {
        private const val KEY_TREE_URI = "tree_uri"
        private const val SUPPORTED_SCHEMA_VERSION = 2
    }
}
