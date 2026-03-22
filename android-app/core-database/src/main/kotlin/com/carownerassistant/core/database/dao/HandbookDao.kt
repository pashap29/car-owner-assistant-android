package com.carownerassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.carownerassistant.core.database.entity.HandbookDocumentEntity
import com.carownerassistant.core.database.entity.HandbookSectionEntity
import com.carownerassistant.core.database.entity.VehicleHandbookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HandbookDao {
    @Query("SELECT * FROM vehicle_handbook WHERE vehicleId = :vehicleId")
    fun observeHandbook(vehicleId: String): Flow<VehicleHandbookEntity?>

    @Query("SELECT * FROM handbook_section WHERE vehicleId = :vehicleId")
    fun observeSections(vehicleId: String): Flow<List<HandbookSectionEntity>>

    @Query("SELECT * FROM handbook_document WHERE vehicleId = :vehicleId ORDER BY createdAtEpochMillis DESC, documentId DESC")
    fun observeDocuments(vehicleId: String): Flow<List<HandbookDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHandbook(entity: VehicleHandbookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSection(entity: HandbookSectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(entity: HandbookDocumentEntity)

    @Query("SELECT * FROM vehicle_handbook WHERE vehicleId = :vehicleId")
    suspend fun getHandbook(vehicleId: String): VehicleHandbookEntity?
}
