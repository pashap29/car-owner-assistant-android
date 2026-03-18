package com.carownerassistant.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.carownerassistant.core.database.dao.FuelDao
import com.carownerassistant.core.database.dao.ExpenseDao
import com.carownerassistant.core.database.dao.MileageDao
import com.carownerassistant.core.database.dao.VehicleDao
import com.carownerassistant.core.database.entity.ExpenseEntryEntity
import com.carownerassistant.core.database.entity.FuelEntryEntity
import com.carownerassistant.core.database.entity.MileageEntryEntity
import com.carownerassistant.core.database.entity.VehicleEntity

@Database(
    entities = [VehicleEntity::class, MileageEntryEntity::class, FuelEntryEntity::class, ExpenseEntryEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun mileageDao(): MileageDao
    abstract fun fuelDao(): FuelDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        const val NAME: String = "car_owner_assistant.db"

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE vehicle ADD COLUMN make TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE vehicle ADD COLUMN model TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE vehicle ADD COLUMN year INTEGER")
                database.execSQL("ALTER TABLE vehicle ADD COLUMN plateNumber TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE vehicle ADD COLUMN createdAtEpochMillis INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE vehicle ADD COLUMN updatedAtEpochMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE mileage_entry ADD COLUMN origin TEXT NOT NULL DEFAULT 'DEDICATED_MILEAGE'",
                )
                database.execSQL(
                    "ALTER TABLE mileage_entry ADD COLUMN userEnteredValue REAL NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE mileage_entry ADD COLUMN userEnteredUnit TEXT NOT NULL DEFAULT 'KM'",
                )
                database.execSQL(
                    "ALTER TABLE mileage_entry ADD COLUMN photoFilePath TEXT",
                )
                database.execSQL(
                    "ALTER TABLE mileage_entry ADD COLUMN manualEntry INTEGER NOT NULL DEFAULT 1",
                )
                database.execSQL(
                    "ALTER TABLE mileage_entry ADD COLUMN largeJumpSuspected INTEGER NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE mileage_entry ADD COLUMN trustedReferenceMileageEntryId TEXT",
                )
                database.execSQL(
                    "ALTER TABLE mileage_entry ADD COLUMN trustScore INTEGER NOT NULL DEFAULT 0",
                )
                database.execSQL(
                    "ALTER TABLE mileage_entry ADD COLUMN anomalyFlagsCsv TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS expense_entry (
                        expenseEntryId TEXT NOT NULL PRIMARY KEY,
                        vehicleId TEXT NOT NULL,
                        timestampEpochMillis INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        totalAmount REAL NOT NULL,
                        note TEXT NOT NULL,
                        attachmentPath TEXT,
                        odometerKm REAL,
                        odometerMi REAL
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
