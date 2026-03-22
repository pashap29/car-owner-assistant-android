package com.carownerassistant.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "service_part_item")
data class ServicePartItemEntity(
    @PrimaryKey val servicePartItemId: String,
    val serviceEntryId: String,
    val title: String,
    val quantity: Int,
    val totalAmount: Double,
)
