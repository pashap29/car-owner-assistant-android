package com.carownerassistant.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.carownerassistant.core.database.entity.PlaceFavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceFavoriteDao {
    @Query("SELECT * FROM place_favorite ORDER BY placeType ASC, displayName ASC")
    fun observeAll(): Flow<List<PlaceFavoriteEntity>>

    @Query("SELECT normalizedPlaceId FROM place_favorite")
    suspend fun getFavoriteIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PlaceFavoriteEntity)

    @Query("DELETE FROM place_favorite WHERE normalizedPlaceId = :normalizedPlaceId")
    suspend fun deleteById(normalizedPlaceId: String)
}
