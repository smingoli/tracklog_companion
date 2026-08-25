package com.smingoli.tracklogcompanion.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import android.util.AtomicFile
import java.io.File
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class CatalogStorage(private val context: Context) {
    private val preferences = context.getSharedPreferences("catalog", Context.MODE_PRIVATE)
    private val catalogDirectory = File(context.filesDir, "catalogue")
    val activeDatabase = File(catalogDirectory, "catalog.db")
    private val pendingDatabase = File(catalogDirectory, "catalog.pending.db")
    private val pendingZip = File(catalogDirectory, "import.pending.zip")

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

    fun importZip(zipUri: Uri, destinationTreeUri: Uri): CatalogSnapshot {
        catalogDirectory.mkdirs()
        pendingDatabase.delete()
        pendingZip.delete()

        try {
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                pendingZip.outputStream().buffered().use { output ->
                    input.copyToLimited(output, MAX_ZIP_BYTES)
                }
            } ?: error("The selected ZIP file could not be opened")

            ZipFile(pendingZip).use { archive ->
                val entries = archive.entries().toList()
                validateArchiveEntries(entries)

                val databaseEntry = entries.first { normaliseZipEntry(it) == "catalog.db" }
                archive.getInputStream(databaseEntry).use { input ->
                    pendingDatabase.outputStream().buffered().use { output ->
                        input.copyToLimited(output, MAX_DATABASE_BYTES)
                    }
                }
                validate(pendingDatabase)

                entries
                    .filterNot(ZipEntry::isDirectory)
                    .sortedBy { normaliseZipEntry(it) == "catalog.db" }
                    .forEach { entry -> writeArchiveEntry(archive, entry, destinationTreeUri) }
            }

            promotePendingDatabase()
            preferences.edit().putString(KEY_TREE_URI, destinationTreeUri.toString()).apply()
            return CatalogRepository(activeDatabase).readSnapshot(destinationTreeUri)
        } finally {
            pendingDatabase.delete()
            pendingZip.delete()
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

    private fun writeArchiveEntry(archive: ZipFile, entry: ZipEntry, root: Uri) {
        val path = normaliseZipEntry(entry)
        val parts = path.split('/')
        var parent = root
        for (directory in parts.dropLast(1)) parent = ensureDirectory(parent, directory)

        val filename = parts.last()
        val mimeType = when (filename.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
            "db" -> "application/vnd.sqlite3"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
        val target = findChild(parent, filename)
            ?: DocumentsContract.createDocument(context.contentResolver, parent.asDocumentUri(), mimeType, filename)
            ?: error("Could not create $path in the selected folder")

        context.contentResolver.openOutputStream(target, "wt")?.use { output ->
            archive.getInputStream(entry).use { input -> input.copyToLimited(output, MAX_ENTRY_BYTES) }
        } ?: error("Could not write $path in the selected folder")
    }

    private fun ensureDirectory(parent: Uri, name: String): Uri =
        findChild(parent, name)
            ?: DocumentsContract.createDocument(
                context.contentResolver,
                parent.asDocumentUri(),
                DocumentsContract.Document.MIME_TYPE_DIR,
                name,
            )
            ?: error("Could not create the $name folder")

    private fun Uri.asDocumentUri(): Uri =
        if (DocumentsContract.isDocumentUri(context, this)) {
            this
        } else {
            DocumentsContract.buildDocumentUriUsingTree(this, DocumentsContract.getTreeDocumentId(this))
        }

    private fun validateArchiveEntries(entries: List<ZipEntry>) {
        if (entries.isEmpty()) error("The selected ZIP file is empty")
        if (entries.size > MAX_ENTRY_COUNT) error("The selected ZIP contains too many files")

        val normalised = entries.map(::normaliseZipEntry)
        if (normalised.map { it.lowercase(Locale.ROOT) }.toSet().size != normalised.size) {
            error("The selected ZIP contains duplicate file paths")
        }
        if ("catalog.db" !in normalised) error("The selected ZIP does not contain catalog.db")
        val totalBytes = entries.filterNot(ZipEntry::isDirectory).sumOf { entry ->
            if (entry.size < 0) error("The ZIP contains a file with an unknown size")
            if (entry.size > MAX_ENTRY_BYTES) error("The ZIP contains a file that is too large")
            entry.size
        }
        if (totalBytes > MAX_EXTRACTED_BYTES) error("The selected ZIP expands beyond the supported size")
    }

    private fun normaliseZipEntry(entry: ZipEntry): String {
        val path = entry.name.replace('\\', '/').trimEnd('/')
        if (path.isBlank() || path.startsWith('/') || ':' in path) error("The ZIP contains an unsafe file path")
        val parts = path.split('/')
        if (parts.any { it.isBlank() || it == "." || it == ".." }) error("The ZIP contains an unsafe file path")
        val allowed = path == "catalog.db" || path == "images" || path == "images/releases" ||
            path.startsWith("images/releases/")
        if (!allowed) error("The ZIP contains an unsupported entry: $path")
        return path
    }

    private fun java.io.InputStream.copyToLimited(output: java.io.OutputStream, maximumBytes: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var copied = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            copied += count
            if (copied > maximumBytes) error("The selected ZIP exceeds the supported size")
            output.write(buffer, 0, count)
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

            requireColumns(database, "releases", setOf("id", "title", "type", "status", "image_path", "description"))
            requireColumns(database, "tracks", setOf("id", "title", "description", "lyrics", "notes"))
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
        private const val MAX_ENTRY_COUNT = 2_000
        private const val MAX_ZIP_BYTES = 512L * 1024 * 1024
        private const val MAX_EXTRACTED_BYTES = 1024L * 1024 * 1024
        private const val MAX_ENTRY_BYTES = 256L * 1024 * 1024
        private const val MAX_DATABASE_BYTES = 64L * 1024 * 1024
    }
}
