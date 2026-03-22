package com.carownerassistant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_work_item")
data class ServiceWorkItemEntity(
    @PrimaryKey val serviceWorkItemId: String,
    val serviceEntryId: String,
    val title: String,
    val totalAmount: Double,
)
