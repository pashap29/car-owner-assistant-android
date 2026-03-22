package com.carownerassistant.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.carownerassistant.core.database.dao.FuelDao
import com.carownerassistant.core.database.dao.HandbookDao
import com.carownerassistant.core.database.dao.PlaceFavoriteDao
import com.carownerassistant.core.database.dao.ExpenseDao
import com.carownerassistant.core.database.dao.MileageDao
import com.carownerassistant.core.database.dao.ServiceDao
import com.carownerassistant.core.database.dao.VehicleDao
import com.carownerassistant.core.database.entity.ExpenseEntryEntity
import com.carownerassistant.core.database.entity.FuelEntryEntity
import com.carownerassistant.core.database.entity.HandbookDocumentEntity
import com.carownerassistant.core.database.entity.HandbookSectionEntity
import com.carownerassistant.core.database.entity.MileageEntryEntity
import com.carownerassistant.core.database.entity.PlaceFavoriteEntity
import com.carownerassistant.core.database.entity.ServiceEntryEntity
import com.carownerassistant.core.database.entity.ServicePartItemEntity
import com.carownerassistant.core.database.entity.ServiceWorkItemEntity
import com.carownerassistant.core.database.entity.VehicleHandbookEntity
import com.carownerassistant.core.database.entity.VehicleEntity

@Database(
    entities = [
        VehicleEntity::class,
        MileageEntryEntity::class,
        FuelEntryEntity::class,
        ExpenseEntryEntity::class,
        ServiceEntryEntity::class,
        ServiceWorkItemEntity::class,
        ServicePartItemEntity::class,
        VehicleHandbookEntity::class,
        HandbookSectionEntity::class,
        HandbookDocumentEntity::class,
        PlaceFavoriteEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun mileageDao(): MileageDao
    abstract fun fuelDao(): FuelDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun serviceDao(): ServiceDao
    abstract fun handbookDao(): HandbookDao
    abstract fun placeFavoriteDao(): PlaceFavoriteDao

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

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE fuel_entry ADD COLUMN fuelType TEXT NOT NULL DEFAULT 'GASOLINE_95'",
                )
                database.execSQL(
                    "ALTER TABLE fuel_entry ADD COLUMN entryMethod TEXT NOT NULL DEFAULT 'MANUAL'",
                )
                database.execSQL(
                    "ALTER TABLE fuel_entry ADD COLUMN mileageDeltaKm REAL",
                )
                database.execSQL(
                    "ALTER TABLE fuel_entry ADD COLUMN qrPayloadRaw TEXT",
                )
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS service_entry (
                        serviceEntryId TEXT NOT NULL PRIMARY KEY,
                        vehicleId TEXT NOT NULL,
                        timestampEpochMillis INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        totalAmount REAL NOT NULL,
                        address TEXT,
                        phone TEXT,
                        contact TEXT,
                        odometerKm REAL,
                        odometerMi REAL,
                        linkedMileageEntryId TEXT
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS service_work_item (
                        serviceWorkItemId TEXT NOT NULL PRIMARY KEY,
                        serviceEntryId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        totalAmount REAL NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS service_part_item (
                        servicePartItemId TEXT NOT NULL PRIMARY KEY,
                        serviceEntryId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        totalAmount REAL NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vehicle_handbook (
                        vehicleId TEXT NOT NULL PRIMARY KEY,
                        vin TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS handbook_section (
                        handbookSectionId TEXT NOT NULL PRIMARY KEY,
                        vehicleId TEXT NOT NULL,
                        sectionId TEXT NOT NULL,
                        content TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS handbook_document (
                        documentId TEXT NOT NULL PRIMARY KEY,
                        vehicleId TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        filePath TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS place_favorite (
                        normalizedPlaceId TEXT NOT NULL PRIMARY KEY,
                        providerKey TEXT NOT NULL,
                        providerPlaceId TEXT NOT NULL,
                        placeType TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        formattedAddress TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        phone TEXT,
                        rating REAL,
                        reviewCount INTEGER,
                        tagsCsv TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
