package com.carownerassistant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "handbook_document")
data class HandbookDocumentEntity(
    @PrimaryKey val documentId: String,
    val vehicleId: String,
    val displayName: String,
    val mimeType: String,
    val filePath: String,
    val createdAtEpochMillis: Long,
)
