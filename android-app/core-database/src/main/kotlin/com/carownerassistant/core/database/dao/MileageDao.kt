package com.carownerassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.carownerassistant.core.database.entity.MileageEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MileageDao {
    @Query("SELECT * FROM mileage_entry WHERE vehicleId = :vehicleId ORDER BY timestampEpochMillis ASC, mileageEntryId ASC")
    fun observeForVehicle(vehicleId: String): Flow<List<MileageEntryEntity>>

    @Query("SELECT * FROM mileage_entry WHERE vehicleId = :vehicleId ORDER BY timestampEpochMillis ASC, mileageEntryId ASC")
    suspend fun getForVehicle(vehicleId: String): List<MileageEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<MileageEntryEntity>)

    @Query("DELETE FROM mileage_entry WHERE mileageEntryId = :mileageEntryId")
    suspend fun deleteById(mileageEntryId: String)
}
