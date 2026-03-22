package com.carownerassistant.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelRulesTest {

    @Test
    fun `qr parser extracts amount and timestamp`() {
        val prefill = FuelQrParser.parse("t=20260318T2230&s=2499.90&fn=123")

        assertNotNull(prefill.totalAmount)
        assertEquals(2499.90, prefill.totalAmount!!, 0.0001)
        assertNotNull(prefill.timestampEpochMillis)
    }

    @Test
    fun `blank qr payload keeps fields empty`() {
        val prefill = FuelQrParser.parse("")

        assertNull(prefill.timestampEpochMillis)
        assertNull(prefill.totalAmount)
    }

    @Test
    fun `most frequent fuel type is suggested`() {
        val suggested = FuelRules.suggestFuelType(
            listOf(
                fuel(fuelType = FuelType.DIESEL, timestamp = 1L),
                fuel(fuelType = FuelType.GASOLINE_95, timestamp = 2L),
                fuel(fuelType = FuelType.DIESEL, timestamp = 3L),
            ),
        )

        assertEquals(FuelType.DIESEL, suggested)
    }

    @Test
    fun `mileage impact draft is created from fuel context`() {
        val linkedMileage = FuelRules.buildLinkedMileageDraft(
            FuelEntryDraft(
                vehicleId = "vehicle-1",
                timestampEpochMillis = 100L,
                liters = 30.0,
                totalAmount = 1500.0,
                fuelType = FuelType.GASOLINE_95,
                isFullTank = true,
                odometerValue = 1200.0,
                odometerUnit = DistanceUnit.KM,
                entryMethod = FuelEntryMethod.MANUAL,
                qrPayloadRaw = null,
            ),
        )

        assertEquals(MileageEntryOrigin.FUEL_CONTEXT, linkedMileage.origin)
        assertEquals(1200.0, linkedMileage.value, 0.0)
        assertEquals(DistanceUnit.KM, linkedMileage.inputUnit)
        assertTrue(linkedMileage.manualEntry)
        assertNull(linkedMileage.photoFilePath)
    }

    @Test
    fun `delta is computed from previous mileage`() {
        val delta = FuelRules.computeMileageDeltaKm(
            previousMileageKm = 1000.0,
            newReading = OdometerReading(
                km = 1245.0,
                mi = 773.663395,
            ),
        )

        assertNotNull(delta)
        assertEquals(245.0, delta!!, 0.0001)
    }

    @Test
    fun `draft validation requires positive liters amount and odometer`() {
        val result = FuelRules.validateDraft(
            liters = 40.0,
            totalAmount = 2100.0,
            odometerValue = 15000.0,
            odometerUnit = DistanceUnit.KM,
        )

        assertTrue(result is FuelDraftValidationResult.Valid)
    }

    private fun fuel(
        fuelType: FuelType,
        timestamp: Long,
    ): FuelEntrySummary {
        return FuelEntrySummary(
            id = "fuel-$timestamp",
            vehicleId = "vehicle-1",
            timestampEpochMillis = timestamp,
            liters = 30.0,
            totalAmount = 1500.0,
            isFullTank = true,
            fuelType = fuelType,
            entryMethod = FuelEntryMethod.MANUAL,
            odometerReading = OdometerReading(
                km = 1000.0 + timestamp,
                mi = (1000.0 + timestamp) * 0.621371,
            ),
            mileageDeltaKm = 100.0,
            qrPayloadRaw = null,
        )
    }
}
