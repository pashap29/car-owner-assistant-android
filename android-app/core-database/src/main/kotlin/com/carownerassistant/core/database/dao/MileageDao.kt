package com.carownerassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.carownerassistant.core.database.entity.MileageEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MileageDao {
    @Query("SELECT * FROM mileage_entry WHERE vehicleId = :vehicleId ORDER BY timestampEpochMillis ASC")
    fun observeForVehicle(vehicleId: String): Flow<List<MileageEntryEntity>>
}
