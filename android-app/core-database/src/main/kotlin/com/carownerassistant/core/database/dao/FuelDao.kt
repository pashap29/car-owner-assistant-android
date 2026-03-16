package com.carownerassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.carownerassistant.core.database.entity.FuelEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entry WHERE vehicleId = :vehicleId ORDER BY timestampEpochMillis ASC")
    fun observeForVehicle(vehicleId: String): Flow<List<FuelEntryEntity>>
}
