package com.carownerassistant.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MileageLedgerEvaluatorTest {

    @Test
    fun `unit converter stores km and miles together`() {
        val reading = MileageUnitConverter.toReading(
            value = 100.0,
            inputUnit = DistanceUnit.MI,
        )

        assertEquals(160.934, reading.km, 0.001)
        assertEquals(100.0, reading.mi, 0.001)
    }

    @Test
    fun `entry with photo and manual input becomes verified`() {
        val evaluated = MileageLedgerEvaluator.evaluate(
            entries = listOf(
                rawEntry(
                    id = "m1",
                    timestamp = day(1),
                    valueKm = 10_000.0,
                    photoFilePath = "photo.jpg",
                ),
            ),
        )

        assertEquals(MileageStatus.VERIFIED, evaluated.single().status)
        assertEquals(100, evaluated.single().trustScore)
    }

    @Test
    fun `manual entry without photo becomes unverified`() {
        val evaluated = MileageLedgerEvaluator.evaluate(
            entries = listOf(
                rawEntry(
                    id = "m1",
                    timestamp = day(1),
                    valueKm = 10_000.0,
                    photoFilePath = null,
                ),
            ),
        )

        assertEquals(MileageStatus.UNVERIFIED, evaluated.single().status)
        assertEquals(60, evaluated.single().trustScore)
    }

    @Test
    fun `regression outside nearby tolerance becomes conflicted`() {
        val evaluated = MileageLedgerEvaluator.evaluate(
            entries = listOf(
                rawEntry(id = "m1", timestamp = day(1), valueKm = 10_000.0, photoFilePath = "1.jpg"),
                rawEntry(id = "m2", timestamp = day(2), valueKm = 9_200.0, photoFilePath = "2.jpg"),
            ),
        )

        val conflictedEntry = evaluated.last()
        assertEquals(MileageStatus.CONFLICTED, conflictedEntry.status)
        assertTrue(conflictedEntry.criticalAnomaly)
        assertTrue(conflictedEntry.anomalyTypes.contains(MileageAnomalyType.REGRESSION))
    }

    @Test
    fun `small nearby regression is tolerated and stays unverified or verified`() {
        val evaluated = MileageLedgerEvaluator.evaluate(
            entries = listOf(
                rawEntry(id = "m1", timestamp = day(1), valueKm = 10_000.0, photoFilePath = "1.jpg"),
                rawEntry(id = "m2", timestamp = day(2), valueKm = 9_700.0, photoFilePath = "2.jpg"),
            ),
        )

        val toleratedEntry = evaluated.last()
        assertEquals(MileageStatus.VERIFIED, toleratedEntry.status)
        assertFalse(toleratedEntry.criticalAnomaly)
        assertTrue(toleratedEntry.anomalyTypes.contains(MileageAnomalyType.NEARBY_TOLERANCE_APPLIED))
    }

    @Test
    fun `same-day difference above 2000 km is conflicted`() {
        val evaluated = MileageLedgerEvaluator.evaluate(
            entries = listOf(
                rawEntry(id = "m1", timestamp = timestamp(1, 10), valueKm = 10_000.0, photoFilePath = "1.jpg"),
                rawEntry(id = "m2", timestamp = timestamp(1, 18), valueKm = 12_500.0, photoFilePath = "2.jpg"),
            ),
        )

        val conflictedEntry = evaluated.last()
        assertEquals(MileageStatus.CONFLICTED, conflictedEntry.status)
        assertTrue(conflictedEntry.anomalyTypes.contains(MileageAnomalyType.SAME_DAY_CONFLICT))
    }

    @Test
    fun `large jump is flagged but not conflicted`() {
        val evaluated = MileageLedgerEvaluator.evaluate(
            entries = listOf(
                rawEntry(id = "m1", timestamp = day(1), valueKm = 10_000.0, photoFilePath = "1.jpg"),
                rawEntry(id = "m2", timestamp = day(4), valueKm = 16_000.0, photoFilePath = "2.jpg"),
            ),
        )

        val suspiciousEntry = evaluated.last()
        assertEquals(MileageStatus.VERIFIED, suspiciousEntry.status)
        assertTrue(suspiciousEntry.largeJumpSuspected)
        assertEquals(80, suspiciousEntry.trustScore)
    }

    @Test
    fun `ledger summary exposes anomaly log and average trust score`() {
        val entries = MileageLedgerEvaluator.evaluate(
            entries = listOf(
                rawEntry(id = "m1", timestamp = timestamp(1, 10), valueKm = 10_000.0, photoFilePath = "1.jpg"),
                rawEntry(id = "m2", timestamp = timestamp(1, 18), valueKm = 10_120.0, photoFilePath = null),
            ),
        )

        val ledger = MileageLedgerEvaluator.toLedger(entries)

        assertEquals(80, ledger.overallTrustScore)
        assertTrue(ledger.anomalyLog.isNotEmpty())
    }

    private fun rawEntry(
        id: String,
        timestamp: Long,
        valueKm: Double,
        photoFilePath: String?,
    ): RawMileageEntry {
        val reading = MileageUnitConverter.toReading(
            value = valueKm,
            inputUnit = DistanceUnit.KM,
        )

        return RawMileageEntry(
            id = id,
            vehicleId = "vehicle-1",
            timestampEpochMillis = timestamp,
            odometerKm = reading.km,
            odometerMi = reading.mi,
            origin = MileageEntryOrigin.DEDICATED_MILEAGE,
            userEnteredValue = valueKm,
            userEnteredUnit = DistanceUnit.KM,
            photoFilePath = photoFilePath,
            manualEntry = true,
        )
    }

    private fun day(dayNumber: Int): Long = timestamp(dayNumber, 12)

    private fun timestamp(
        dayNumber: Int,
        hour: Int,
    ): Long {
        return ((dayNumber - 1) * 24L + hour.toLong()) * 60L * 60L * 1000L
    }
}
