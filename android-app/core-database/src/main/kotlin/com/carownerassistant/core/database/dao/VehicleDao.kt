package com.carownerassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.carownerassistant.core.database.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicle ORDER BY isActive DESC, displayName ASC")
    fun observeVehicles(): Flow<List<VehicleEntity>>

    @Query("SELECT vehicleId FROM vehicle WHERE isActive = 1 LIMIT 1")
    fun observeActiveVehicleId(): Flow<String?>

    @Query("SELECT vehicleId FROM vehicle WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveVehicleId(): String?

    @Query("SELECT * FROM vehicle WHERE vehicleId = :vehicleId LIMIT 1")
    suspend fun getVehicle(vehicleId: String): VehicleEntity?

    @Query("SELECT vehicleId FROM vehicle ORDER BY createdAtEpochMillis ASC LIMIT 1")
    suspend fun getFirstVehicleId(): String?

    @Query("SELECT * FROM vehicle ORDER BY createdAtEpochMillis ASC")
    suspend fun observeVehiclesOnce(): List<VehicleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VehicleEntity)

    @Update
    suspend fun update(entity: VehicleEntity)

    @Query("DELETE FROM vehicle WHERE vehicleId = :vehicleId")
    suspend fun deleteById(vehicleId: String)

    @Query("UPDATE vehicle SET isActive = 0 WHERE isActive = 1")
    suspend fun clearActive()

    @Query("UPDATE vehicle SET isActive = 1, updatedAtEpochMillis = :updatedAtEpochMillis WHERE vehicleId = :vehicleId")
    suspend fun setActive(vehicleId: String, updatedAtEpochMillis: Long)
}
