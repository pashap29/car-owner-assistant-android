package com.carownerassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.carownerassistant.core.database.entity.ExpenseEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expense_entry WHERE vehicleId = :vehicleId ORDER BY timestampEpochMillis DESC, expenseEntryId DESC")
    fun observeForVehicle(vehicleId: String): Flow<List<ExpenseEntryEntity>>

    @Query("SELECT * FROM expense_entry WHERE vehicleId = :vehicleId ORDER BY timestampEpochMillis DESC, expenseEntryId DESC")
    suspend fun getForVehicle(vehicleId: String): List<ExpenseEntryEntity>

    @Query("SELECT * FROM expense_entry WHERE expenseEntryId = :expenseEntryId LIMIT 1")
    suspend fun getById(expenseEntryId: String): ExpenseEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExpenseEntryEntity)

    @Update
    suspend fun update(entity: ExpenseEntryEntity)

    @Query("DELETE FROM expense_entry WHERE expenseEntryId = :expenseEntryId")
    suspend fun deleteById(expenseEntryId: String)
}
