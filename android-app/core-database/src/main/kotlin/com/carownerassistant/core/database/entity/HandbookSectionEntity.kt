package com.carownerassistant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "handbook_section")
data class HandbookSectionEntity(
    @PrimaryKey val handbookSectionId: String,
    val vehicleId: String,
    val sectionId: String,
    val content: String,
)
