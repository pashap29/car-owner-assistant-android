package com.carownerassistant.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsCalculatorTest {

    private val range = AnalyticsTimeRange(
        startEpochMillis = 1_700_000_000_000,
        endExclusiveEpochMillis = 1_800_000_000_000,
        label = "Test",
    )

    @Test
    fun `expense metrics respect thresholds`() {
        val summary = AnalyticsCalculator.buildDashboard(
            scopeMode = AnalyticsScopeMode.ACTIVE_CAR,
            range = range,
            selectedVehicleIds = listOf("car-1"),
            mileageEntriesByVehicle = emptyMap(),
            fuelEntriesByVehicle = emptyMap(),
            expenseEntriesByVehicle = mapOf(
                "car-1" to listOf(
                    expense(amount = 100.0),
                    expense(amount = 200.0),
                ),
            ),
            serviceEntriesByVehicle = emptyMap(),
        )

        assertEquals(300.0, summary.totalExpenses.value!!, 0.001)
        assertNull(summary.averageCheck.value)
        assertNull(summary.categoryBreakdown.value)
    }

    @Test
    fun `cost per km aggregates across all cars`() {
        val summary = AnalyticsCalculator.buildDashboard(
            scopeMode = AnalyticsScopeMode.ALL_CARS,
            range = range,
            selectedVehicleIds = listOf("car-1", "car-2"),
            mileageEntriesByVehicle = mapOf(
                "car-1" to listOf(mileage("m1", "car-1", 1000.0, 1), mileage("m2", "car-1", 1080.0, 2)),
                "car-2" to listOf(mileage("m3", "car-2", 5000.0, 1), mileage("m4", "car-2", 5070.0, 2)),
            ),
            fuelEntriesByVehicle = emptyMap(),
            expenseEntriesByVehicle = mapOf(
                "car-1" to listOf(expense(amount = 300.0)),
                "car-2" to listOf(expense(amount = 150.0)),
            ),
            serviceEntriesByVehicle = emptyMap(),
        )

        assertEquals(150.0, summary.totalDistanceKm.value!!, 0.001)
        assertEquals(3.0, summary.costPerKm.value!!, 0.001)
    }

    @Test
    fun `fuel analytics use only valid closed chains`() {
        val summary = AnalyticsCalculator.buildDashboard(
            scopeMode = AnalyticsScopeMode.ACTIVE_CAR,
            range = range,
            selectedVehicleIds = listOf("car-1"),
            mileageEntriesByVehicle = mapOf(
                "car-1" to listOf(
                    mileage("m1", "car-1", 1000.0, 1),
                    mileage("m2", "car-1", 1100.0, 2),
                ),
            ),
            fuelEntriesByVehicle = mapOf(
                "car-1" to listOf(
                    fuel("f1", "car-1", liters = 40.0, totalAmount = 2000.0, odometerKm = 1000.0, timestampShift = 1, isFullTank = true),
                    fuel("f2", "car-1", liters = 20.0, totalAmount = 1000.0, odometerKm = 1050.0, timestampShift = 2, isFullTank = false),
                    fuel("f3", "car-1", liters = 30.0, totalAmount = 1500.0, odometerKm = 1100.0, timestampShift = 3, isFullTank = true),
                ),
            ),
            expenseEntriesByVehicle = emptyMap(),
            serviceEntriesByVehicle = emptyMap(),
        )

        assertEquals(1, summary.validFuelChains)
        assertEquals(50.0, summary.fuelConsumptionLPer100Km.value!!, 0.001)
        assertEquals(2500.0, summary.fuelCostPer100Km.value!!, 0.001)
    }

    @Test
    fun `trend requires at least two populated buckets`() {
        val summary = AnalyticsCalculator.buildDashboard(
            scopeMode = AnalyticsScopeMode.ACTIVE_CAR,
            range = AnalyticsTimeRange(
                startEpochMillis = 1_700_000_000_000,
                endExclusiveEpochMillis = 1_700_086_400_000,
                label = "1 day",
            ),
            selectedVehicleIds = listOf("car-1"),
            mileageEntriesByVehicle = emptyMap(),
            fuelEntriesByVehicle = emptyMap(),
            expenseEntriesByVehicle = mapOf("car-1" to listOf(expense(amount = 100.0, timestamp = 1_700_010_000_000))),
            serviceEntriesByVehicle = emptyMap(),
        )

        assertNull(summary.trend.value)
        assertNotNull(summary.trend.emptyStateMessage)
    }

    private fun expense(
        amount: Double,
        timestamp: Long = 1_700_001_000_000,
    ): ExpenseEntrySummary {
        return ExpenseEntrySummary(
            id = "e-$amount-$timestamp",
            vehicleId = "car-1",
            timestampEpochMillis = timestamp,
            category = ExpenseCategory.OTHER,
            totalAmount = amount,
            note = "",
            attachmentPath = null,
            mileageReading = null,
        )
    }

    private fun mileage(
        id: String,
        vehicleId: String,
        odometerKm: Double,
        dayOffset: Int,
    ): MileageEntrySummary {
        return MileageEntrySummary(
            id = id,
            vehicleId = vehicleId,
            timestampEpochMillis = 1_700_000_000_000 + (dayOffset * 86_400_000L),
            reading = OdometerReading(km = odometerKm, mi = odometerKm * 0.621371),
            status = MileageStatus.VERIFIED,
            origin = MileageEntryOrigin.DEDICATED_MILEAGE,
            userEnteredValue = odometerKm,
            userEnteredUnit = DistanceUnit.KM,
            photoFilePath = "photo.jpg",
            manualEntry = true,
            criticalAnomaly = false,
            largeJumpSuspected = false,
            trustedReferenceEntryId = null,
            trustScore = 100,
            anomalyTypes = emptyList(),
        )
    }

    private fun fuel(
        id: String,
        vehicleId: String,
        liters: Double,
        totalAmount: Double,
        odometerKm: Double,
        timestampShift: Int,
        isFullTank: Boolean,
    ): FuelEntrySummary {
        return FuelEntrySummary(
            id = id,
            vehicleId = vehicleId,
            timestampEpochMillis = 1_700_000_000_000 + (timestampShift * 86_400_000L),
            liters = liters,
            totalAmount = totalAmount,
            isFullTank = isFullTank,
            fuelType = FuelType.GASOLINE_95,
            entryMethod = FuelEntryMethod.MANUAL,
            odometerReading = OdometerReading(km = odometerKm, mi = odometerKm * 0.621371),
            mileageDeltaKm = null,
            qrPayloadRaw = null,
        )
    }
}
