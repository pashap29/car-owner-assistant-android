package com.carownerassistant.core.files

import android.content.Context
import java.io.File

enum class FileBucket(val relativePath: String) {
    MEDIA_MILEAGE("media/mileage"),
    MEDIA_RECEIPTS("media/receipts"),
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
