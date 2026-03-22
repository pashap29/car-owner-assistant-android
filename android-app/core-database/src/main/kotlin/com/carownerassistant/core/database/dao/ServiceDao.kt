package com.carownerassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.carownerassistant.core.database.entity.ServiceEntryEntity
import com.carownerassistant.core.database.entity.ServicePartItemEntity
import com.carownerassistant.core.database.entity.ServiceWorkItemEntity
import kotlinx.coroutines.flow.Flow

data class ServiceEntryAggregate(
    @Embedded val entry: ServiceEntryEntity,
    @Relation(
        parentColumn = "serviceEntryId",
        entityColumn = "serviceEntryId",
    )
    val workItems: List<ServiceWorkItemEntity>,
    @Relation(
        parentColumn = "serviceEntryId",
        entityColumn = "serviceEntryId",
    )
    val partItems: List<ServicePartItemEntity>,
)

@Dao
interface ServiceDao {
    @Transaction
    @Query("SELECT * FROM service_entry WHERE vehicleId = :vehicleId ORDER BY timestampEpochMillis DESC, serviceEntryId DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<ServiceEntryAggregate>>

    @Transaction
    @Query("SELECT * FROM service_entry WHERE serviceEntryId = :serviceEntryId")
    suspend fun getById(serviceEntryId: String): ServiceEntryAggregate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ServiceEntryEntity)

    @Update
    suspend fun updateEntry(entry: ServiceEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkItems(entries: List<ServiceWorkItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartItems(entries: List<ServicePartItemEntity>)

    @Query("DELETE FROM service_work_item WHERE serviceEntryId = :serviceEntryId")
    suspend fun deleteWorkItemsForService(serviceEntryId: String)

    @Query("DELETE FROM service_part_item WHERE serviceEntryId = :serviceEntryId")
    suspend fun deletePartItemsForService(serviceEntryId: String)
}
