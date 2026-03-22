package com.carownerassistant.core.files

import android.content.Context
import android.net.Uri
import java.io.File

enum class FileBucket(val relativePath: String) {
    MEDIA_MILEAGE("media/mileage"),
    MEDIA_RECEIPTS("media/receipts"),
    MEDIA_DOCUMENTS("media/documents"),
    BACKUP("backup"),
    IMPORT_TEMP("cache/import-temp"),
    SCAN_TEMP("cache/scan-temp"),
}

interface AppFileStore {
    fun bucket(bucket: FileBucket): File
    fun backupNewFile(): File
    fun backupOldFile(): File
}

class AndroidAppFileStore(
    private val context: Context,
) : AppFileStore {

    override fun bucket(bucket: FileBucket): File {
        val base = if (bucket.relativePath.startsWith("cache/")) context.cacheDir else context.filesDir
        val cleanPath = bucket.relativePath.removePrefix("cache/")
        return File(base, cleanPath).apply { mkdirs() }
    }

    override fun backupNewFile(): File = File(bucket(FileBucket.BACKUP), "backup-new.zip")

    override fun backupOldFile(): File = File(bucket(FileBucket.BACKUP), "backup-old.zip")
}

fun copyContentUriToFile(
    context: Context,
    sourceUri: Uri,
    destinationFile: File,
): String? {
    return runCatching {
        destinationFile.parentFile?.mkdirs()
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return null

        destinationFile.absolutePath
    }.getOrNull()
}
