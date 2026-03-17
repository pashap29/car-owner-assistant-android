package com.carownerassistant.core.model

import kotlin.math.abs
import kotlin.math.round

data class RawMileageEntry(
    val id: String,
    val vehicleId: String,
    val timestampEpochMillis: Long,
    val odometerKm: Double,
    val odometerMi: Double,
    val origin: MileageEntryOrigin,
    val userEnteredValue: Double,
    val userEnteredUnit: DistanceUnit,
    val photoFilePath: String?,
    val manualEntry: Boolean,
)

object MileageUnitConverter {
    private const val KM_TO_MI = 0.621371

    fun toReading(
        value: Double,
        inputUnit: DistanceUnit,
    ): OdometerReading {
        val km = when (inputUnit) {
            DistanceUnit.KM -> value
            DistanceUnit.MI -> value / KM_TO_MI
        }
        val mi = when (inputUnit) {
            DistanceUnit.KM -> value * KM_TO_MI
            DistanceUnit.MI -> value
        }

        return OdometerReading(
            km = roundToThreeDecimals(km),
            mi = roundToThreeDecimals(mi),
        )
    }

    private fun roundToThreeDecimals(value: Double): Double {
        return round(value * 1000.0) / 1000.0
    }
}

object MileageLedgerEvaluator {
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun evaluate(entries: List<RawMileageEntry>): List<MileageEntrySummary> {
        val orderedEntries = entries.sortedWith(
            compareBy<RawMileageEntry> { it.timestampEpochMillis }.thenBy { it.id },
        )

        val evaluated = mutableListOf<MileageEntrySummary>()
        var previousTrusted: MileageEntrySummary? = null

        orderedEntries.forEach { entry ->
            val anomalyTypes = mutableListOf<MileageAnomalyType>()
            var criticalAnomaly = false
            var largeJumpSuspected = false

            previousTrusted?.let { trusted ->
                val deltaKm = entry.odometerKm - trusted.reading.km
                val absoluteDeltaKm = abs(deltaKm)
                val dayDistance = abs(entry.timestampEpochMillis - trusted.timestampEpochMillis) / MILLIS_PER_DAY
                val sameDay = isSameUtcDay(entry.timestampEpochMillis, trusted.timestampEpochMillis)

                if (sameDay && absoluteDeltaKm > 2000.0) {
                    criticalAnomaly = true
                    anomalyTypes += MileageAnomalyType.SAME_DAY_CONFLICT
                } else if (sameDay && absoluteDeltaKm > 50.0) {
                    anomalyTypes += MileageAnomalyType.SAME_DAY_VARIANCE
                }

                if (deltaKm < 0.0) {
                    val nearbyToleranceApplied =
                        dayDistance <= 7L &&
                            absoluteDeltaKm <= 500.0 &&
                            absoluteDeltaKm <= entry.odometerKm * 0.05

                    if (nearbyToleranceApplied) {
                        anomalyTypes += MileageAnomalyType.NEARBY_TOLERANCE_APPLIED
                    } else {
                        criticalAnomaly = true
                        anomalyTypes += MileageAnomalyType.REGRESSION
                    }
                }

                if (deltaKm > 0.0) {
                    largeJumpSuspected =
                        (dayDistance in 1L..3L && deltaKm > 5000.0) ||
                            (dayDistance in 1L..30L && deltaKm > 100000.0) ||
                            (dayDistance >= 3L && deltaKm / dayDistance.toDouble() > 1000.0)

                    if (largeJumpSuspected) {
                        anomalyTypes += MileageAnomalyType.LARGE_JUMP_SUSPECTED
                    }
                }
            }

            val status = when {
                criticalAnomaly -> MileageStatus.CONFLICTED
                isVerified(entry) -> MileageStatus.VERIFIED
                else -> MileageStatus.UNVERIFIED
            }

            val trustScore = when (status) {
                MileageStatus.VERIFIED -> if (largeJumpSuspected) 80 else 100
                MileageStatus.UNVERIFIED -> if (largeJumpSuspected) 40 else 60
                MileageStatus.CONFLICTED -> 0
            }

            val summary = MileageEntrySummary(
                id = entry.id,
                vehicleId = entry.vehicleId,
                timestampEpochMillis = entry.timestampEpochMillis,
                reading = OdometerReading(
                    km = entry.odometerKm,
                    mi = entry.odometerMi,
                ),
                status = status,
                origin = entry.origin,
                userEnteredValue = entry.userEnteredValue,
                userEnteredUnit = entry.userEnteredUnit,
                photoFilePath = entry.photoFilePath,
                manualEntry = entry.manualEntry,
                criticalAnomaly = criticalAnomaly,
                largeJumpSuspected = largeJumpSuspected,
                trustedReferenceEntryId = previousTrusted?.id,
                trustScore = trustScore,
                anomalyTypes = anomalyTypes.distinct(),
            )

            evaluated += summary

            if (summary.status != MileageStatus.CONFLICTED) {
                previousTrusted = summary
            }
        }

        return evaluated
    }

    fun toLedger(entries: List<MileageEntrySummary>): MileageLedgerSummary {
        val anomalyLog = entries
            .flatMap { entry ->
                entry.anomalyTypes.map { type ->
                    MileageAnomalyLogItem(
                        mileageEntryId = entry.id,
                        timestampEpochMillis = entry.timestampEpochMillis,
                        type = type,
                        message = messageFor(type),
                    )
                }
            }
            .sortedByDescending { it.timestampEpochMillis }

        val overallTrustScore = if (entries.isEmpty()) {
            0
        } else {
            entries.map { it.trustScore }.average().roundToInt()
        }

        return MileageLedgerSummary(
            entries = entries.sortedByDescending { it.timestampEpochMillis },
            anomalyLog = anomalyLog,
            overallTrustScore = overallTrustScore,
        )
    }

    private fun isVerified(entry: RawMileageEntry): Boolean {
        return entry.origin == MileageEntryOrigin.DEDICATED_MILEAGE &&
            !entry.photoFilePath.isNullOrBlank() &&
            entry.manualEntry
    }

    private fun isSameUtcDay(
        firstTimestamp: Long,
        secondTimestamp: Long,
    ): Boolean {
        return firstTimestamp / MILLIS_PER_DAY == secondTimestamp / MILLIS_PER_DAY
    }

    private fun Double.roundToInt(): Int = round(this).toInt()

    private fun messageFor(type: MileageAnomalyType): String {
        return when (type) {
            MileageAnomalyType.REGRESSION -> "Mileage is lower than the previous trusted reading."
            MileageAnomalyType.SAME_DAY_VARIANCE -> "Same-day mileage differs by more than 50 km."
            MileageAnomalyType.SAME_DAY_CONFLICT -> "Same-day mileage differs by more than 2000 km."
            MileageAnomalyType.LARGE_JUMP_SUSPECTED -> "Large mileage jump suspected."
            MileageAnomalyType.NEARBY_TOLERANCE_APPLIED -> "Small nearby regression tolerated within 7-day window."
        }
    }
}
