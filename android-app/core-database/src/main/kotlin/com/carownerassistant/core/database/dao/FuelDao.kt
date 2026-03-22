package com.carownerassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.carownerassistant.core.database.entity.FuelEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {
    @Query("SELECT * FROM fuel_entry WHERE vehicleId = :vehicleId ORDER BY timestampEpochMillis ASC, fuelEntryId ASC")
    fun observeForVehicle(vehicleId: String): Flow<List<FuelEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: FuelEntryEntity)

    @Query("SELECT * FROM fuel_entry WHERE vehicleId = :vehicleId ORDER BY timestampEpochMillis ASC, fuelEntryId ASC")
    suspend fun getForVehicle(vehicleId: String): List<FuelEntryEntity>
}
