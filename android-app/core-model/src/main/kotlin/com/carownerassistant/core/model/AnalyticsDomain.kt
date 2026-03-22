package com.carownerassistant.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class AnalyticsScopeMode {
    ACTIVE_CAR,
    ALL_CARS,
}

enum class AnalyticsPeriodPreset {
    MONTH,
    HALF_YEAR,
    YEAR,
    CUSTOM,
}

enum class AnalyticsChartMode {
    BAR,
    LINE,
}

enum class FuelChainStatus {
    OPEN,
    CLOSED_VALID,
    CLOSED_INVALID,
    IGNORED,
}

data class AnalyticsTimeRange(
    val startEpochMillis: Long,
    val endExclusiveEpochMillis: Long,
    val label: String,
)

data class AnalyticsMetric<T>(
    val value: T?,
    val trace: String,
    val emptyStateMessage: String? = null,
)

data class CategorySpendSummary(
    val category: ExpenseCategory,
    val totalAmount: Double,
    val percentageOfTotal: Double,
)

data class AnalyticsTrendPoint(
    val bucketStartEpochMillis: Long,
    val label: String,
    val combinedSpendAmount: Double,
    val distanceKm: Double,
)

data class FuelChainSummary(
    val vehicleId: String,
    val startEntryId: String?,
    val endEntryId: String?,
    val endTimestampEpochMillis: Long?,
    val status: FuelChainStatus,
    val distanceKm: Double?,
    val includedLiters: Double,
    val includedAmount: Double,
    val consumptionLPer100Km: Double?,
)

data class AnalyticsDashboardSummary(
    val scopeMode: AnalyticsScopeMode,
    val range: AnalyticsTimeRange,
    val selectedVehicleCount: Int,
    val totalExpenses: AnalyticsMetric<Double>,
    val categoryBreakdown: AnalyticsMetric<List<CategorySpendSummary>>,
    val averageCheck: AnalyticsMetric<Double>,
    val totalDistanceKm: AnalyticsMetric<Double>,
    val verifiedMileageRatioPercent: AnalyticsMetric<Double>,
    val overallTrustScore: AnalyticsMetric<Int>,
    val costPerKm: AnalyticsMetric<Double>,
    val fuelConsumptionLPer100Km: AnalyticsMetric<Double>,
    val fuelCostPer100Km: AnalyticsMetric<Double>,
    val serviceSpend: AnalyticsMetric<Double>,
    val trend: AnalyticsMetric<List<AnalyticsTrendPoint>>,
    val validFuelChains: Int,
)

object AnalyticsPeriodFactory {
    fun forPreset(
        today: LocalDate,
        preset: AnalyticsPeriodPreset,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): AnalyticsTimeRange {
        val endExclusive = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val startDate = when (preset) {
            AnalyticsPeriodPreset.MONTH -> today.minusMonths(1)
            AnalyticsPeriodPreset.HALF_YEAR -> today.minusMonths(6)
            AnalyticsPeriodPreset.YEAR -> today.minusYears(1)
            AnalyticsPeriodPreset.CUSTOM -> today.minusMonths(1)
        }

        return AnalyticsTimeRange(
            startEpochMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endExclusiveEpochMillis = endExclusive,
            label = when (preset) {
                AnalyticsPeriodPreset.MONTH -> "Last month"
                AnalyticsPeriodPreset.HALF_YEAR -> "Last 6 months"
                AnalyticsPeriodPreset.YEAR -> "Last year"
                AnalyticsPeriodPreset.CUSTOM -> "Custom"
            },
        )
    }

    fun custom(
        startDate: LocalDate,
        endDateInclusive: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): AnalyticsTimeRange? {
        if (endDateInclusive.isBefore(startDate)) {
            return null
        }
        return AnalyticsTimeRange(
            startEpochMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endExclusiveEpochMillis = endDateInclusive.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            label = "${startDate} to ${endDateInclusive}",
        )
    }
}

object FuelChainCalculator {
    fun deriveChains(
        fuelEntries: List<FuelEntrySummary>,
        mileageEntries: List<MileageEntrySummary>,
    ): List<FuelChainSummary> {
        if (fuelEntries.isEmpty()) {
            return emptyList()
        }

        val mileageLookup = mileageEntries.associateBy { mileageKey(it.timestampEpochMillis, it.reading.km) }
        val orderedEntries = fuelEntries.sortedWith(
            compareBy<FuelEntrySummary> { it.timestampEpochMillis }.thenBy { it.id },
        )

        val chains = mutableListOf<FuelChainSummary>()
        var currentChainEntries = mutableListOf<FuelEntrySummary>()

        orderedEntries.forEach { entry ->
            if (currentChainEntries.isEmpty()) {
                if (entry.isFullTank) {
                    currentChainEntries = mutableListOf(entry)
                }
                return@forEach
            }

            currentChainEntries += entry
            if (entry.isFullTank) {
                chains += closeChain(
                    currentChainEntries,
                    mileageLookup,
                )
                currentChainEntries = mutableListOf(entry)
            }
        }

        if (currentChainEntries.isNotEmpty()) {
            val start = currentChainEntries.first()
            chains += FuelChainSummary(
                vehicleId = start.vehicleId,
                startEntryId = start.id,
                endEntryId = null,
                endTimestampEpochMillis = null,
                status = FuelChainStatus.OPEN,
                distanceKm = null,
                includedLiters = currentChainEntries.drop(1).sumOf { it.liters },
                includedAmount = currentChainEntries.drop(1).sumOf { it.totalAmount },
                consumptionLPer100Km = null,
            )
        }

        return chains
    }

    private fun closeChain(
        chainEntries: List<FuelEntrySummary>,
        mileageLookup: Map<String, MileageEntrySummary>,
    ): FuelChainSummary {
        val start = chainEntries.first()
        val end = chainEntries.last()
        val includedEntries = chainEntries.drop(1)

        val allHaveOdometer = chainEntries.all { it.odometerReading != null }
        val noDecrease = chainEntries.zipWithNext().all { (left, right) ->
            val leftKm = left.odometerReading?.km ?: return@all false
            val rightKm = right.odometerReading?.km ?: return@all false
            rightKm >= leftKm
        }
        val noIncludedCriticalAnomaly = includedEntries.none { entry ->
            mileageLookup[mileageKey(entry.timestampEpochMillis, entry.odometerReading?.km)]?.criticalAnomaly == true
        }
        val positiveEntryValues = includedEntries.all { it.liters > 0.0 && it.totalAmount > 0.0 }
        val startKm = start.odometerReading?.km
        val endKm = end.odometerReading?.km
        val distanceKm = if (startKm != null && endKm != null) endKm - startKm else null
        val includedLiters = includedEntries.sumOf { it.liters }
        val includedAmount = includedEntries.sumOf { it.totalAmount }

        val valid = allHaveOdometer &&
            noDecrease &&
            noIncludedCriticalAnomaly &&
            positiveEntryValues &&
            distanceKm != null &&
            distanceKm >= 50.0 &&
            includedLiters > 0.0

        return FuelChainSummary(
            vehicleId = start.vehicleId,
            startEntryId = start.id,
            endEntryId = end.id,
            endTimestampEpochMillis = end.timestampEpochMillis,
            status = if (valid) FuelChainStatus.CLOSED_VALID else FuelChainStatus.CLOSED_INVALID,
            distanceKm = distanceKm,
            includedLiters = includedLiters,
            includedAmount = includedAmount,
            consumptionLPer100Km = if (valid && distanceKm != null && distanceKm > 0.0) {
                (includedLiters / distanceKm) * 100.0
            } else {
                null
            },
        )
    }

    private fun mileageKey(
        timestampEpochMillis: Long,
        odometerKm: Double?,
    ): String {
        return "$timestampEpochMillis:${odometerKm ?: "missing"}"
    }
}

object AnalyticsCalculator {
    fun buildDashboard(
        scopeMode: AnalyticsScopeMode,
        range: AnalyticsTimeRange,
        selectedVehicleIds: List<String>,
        mileageEntriesByVehicle: Map<String, List<MileageEntrySummary>>,
        fuelEntriesByVehicle: Map<String, List<FuelEntrySummary>>,
        expenseEntriesByVehicle: Map<String, List<ExpenseEntrySummary>>,
        serviceEntriesByVehicle: Map<String, List<ServiceEntrySummary>>,
    ): AnalyticsDashboardSummary {
        val vehicleIds = selectedVehicleIds.distinct()
        val filteredMileageByVehicle = vehicleIds.associateWith { vehicleId ->
            mileageEntriesByVehicle[vehicleId].orEmpty()
                .filter { it.timestampEpochMillis in range.startEpochMillis until range.endExclusiveEpochMillis }
                .filter { it.status != MileageStatus.CONFLICTED }
                .sortedBy { it.timestampEpochMillis }
        }
        val filteredExpenses = vehicleIds.flatMap { vehicleId ->
            expenseEntriesByVehicle[vehicleId].orEmpty()
                .filter { it.timestampEpochMillis in range.startEpochMillis until range.endExclusiveEpochMillis }
        }
        val filteredServices = vehicleIds.flatMap { vehicleId ->
            serviceEntriesByVehicle[vehicleId].orEmpty()
                .filter { it.timestampEpochMillis in range.startEpochMillis until range.endExclusiveEpochMillis }
        }

        val mileageRecordCount = filteredMileageByVehicle.values.sumOf { it.size }
        val totalDistanceKmValue = filteredMileageByVehicle.values.sumOf { entries ->
            vehicleDistanceKm(entries) ?: 0.0
        }

        val totalExpensesMetric: AnalyticsMetric<Double> = if (filteredExpenses.isNotEmpty()) {
            AnalyticsMetric(
                value = filteredExpenses.sumOf { it.totalAmount },
                trace = "${filteredExpenses.size} expense records in range; total = sum(totalAmount).",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "Threshold requires at least 1 expense record.",
                emptyStateMessage = "Add at least one expense in the selected period.",
            )
        }

        val categoryBreakdownMetric: AnalyticsMetric<List<CategorySpendSummary>> = if (filteredExpenses.size >= 3) {
            val total = filteredExpenses.sumOf { it.totalAmount }
            val breakdown = filteredExpenses
                .groupBy { it.category }
                .map { (category, entries) ->
                    val amount = entries.sumOf { it.totalAmount }
                    CategorySpendSummary(
                        category = category,
                        totalAmount = amount,
                        percentageOfTotal = if (total == 0.0) 0.0 else (amount / total) * 100.0,
                    )
                }
                .sortedByDescending { it.totalAmount }
            AnalyticsMetric(
                value = breakdown,
                trace = "${filteredExpenses.size} expense records; grouped by category with percentage of total spend.",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "Threshold requires at least 3 expense records for category breakdown.",
                emptyStateMessage = "Log at least 3 expenses to unlock category breakdown.",
            )
        }

        val averageCheckMetric: AnalyticsMetric<Double> = if (filteredExpenses.size >= 3) {
            AnalyticsMetric(
                value = filteredExpenses.sumOf { it.totalAmount } / filteredExpenses.size,
                trace = "${filteredExpenses.size} expense records; average = total spend / record count.",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "Threshold requires at least 3 expense records for average check.",
                emptyStateMessage = "Log at least 3 expenses to unlock average check.",
            )
        }

        val totalDistanceMetric: AnalyticsMetric<Double> = if (mileageRecordCount >= 2 && totalDistanceKmValue > 0.0) {
            AnalyticsMetric(
                value = totalDistanceKmValue,
                trace = "$mileageRecordCount trusted mileage records; total distance = sum(last - first) per vehicle.",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "Threshold requires at least 2 non-conflicted mileage records.",
                emptyStateMessage = "Log at least 2 mileage records to unlock mileage summary.",
            )
        }

        val verifiedRatioMetric: AnalyticsMetric<Double> = if (mileageRecordCount >= 2) {
            val verifiedCount = filteredMileageByVehicle.values.flatten().count { it.status == MileageStatus.VERIFIED }
            AnalyticsMetric(
                value = (verifiedCount.toDouble() / mileageRecordCount.toDouble()) * 100.0,
                trace = "$verifiedCount verified mileage records out of $mileageRecordCount trusted records.",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "Threshold requires at least 2 non-conflicted mileage records.",
                emptyStateMessage = "Log at least 2 mileage records to see verified ratio.",
            )
        }

        val overallTrustMetric: AnalyticsMetric<Int> = if (mileageRecordCount >= 2) {
            val averageTrust = filteredMileageByVehicle.values.flatten()
                .map { it.trustScore }
                .average()
                .roundToInt()
            AnalyticsMetric(
                value = averageTrust,
                trace = "$mileageRecordCount trusted mileage records; overall trust = average(entry trustScore).",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "Threshold requires at least 2 non-conflicted mileage records.",
                emptyStateMessage = "Log at least 2 mileage records to see trust score.",
            )
        }

        val costPerKmMetric: AnalyticsMetric<Double> =
            if (filteredExpenses.isNotEmpty() && totalDistanceKmValue >= 50.0 && mileageRecordCount >= 2) {
            AnalyticsMetric(
                value = filteredExpenses.sumOf { it.totalAmount } / totalDistanceKmValue,
                trace = "Cost per km = total expenses in range / trusted distance in range; requires 2 mileage records and >= 50 km.",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "Threshold requires at least 1 expense, 2 mileage records, and >= 50 km distance.",
                emptyStateMessage = "Log expenses and at least 50 km of trusted mileage to unlock cost per km.",
            )
        }

        val validFuelChains = vehicleIds.flatMap { vehicleId ->
            FuelChainCalculator.deriveChains(
                fuelEntries = fuelEntriesByVehicle[vehicleId].orEmpty(),
                mileageEntries = mileageEntriesByVehicle[vehicleId].orEmpty(),
            )
        }.filter { chain ->
            chain.status == FuelChainStatus.CLOSED_VALID &&
                chain.endTimestampEpochMillis != null &&
                chain.endTimestampEpochMillis in range.startEpochMillis until range.endExclusiveEpochMillis
        }

        val fuelConsumptionMetric: AnalyticsMetric<Double> = if (validFuelChains.isNotEmpty()) {
            val totalDistance = validFuelChains.sumOf { it.distanceKm ?: 0.0 }
            val totalLiters = validFuelChains.sumOf { it.includedLiters }
            AnalyticsMetric(
                value = if (totalDistance > 0.0) (totalLiters / totalDistance) * 100.0 else null,
                trace = "${validFuelChains.size} valid closed fuel chains; consumption = sum(liters) / sum(distanceKm) * 100.",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "Threshold requires at least 1 valid closed fuel chain.",
                emptyStateMessage = "Log a full-to-full fuel chain with trusted mileage to unlock fuel analytics.",
            )
        }

        val fuelCostMetric: AnalyticsMetric<Double> = if (validFuelChains.isNotEmpty()) {
            val totalDistance = validFuelChains.sumOf { it.distanceKm ?: 0.0 }
            val totalAmount = validFuelChains.sumOf { it.includedAmount }
            AnalyticsMetric(
                value = if (totalDistance > 0.0) (totalAmount / totalDistance) * 100.0 else null,
                trace = "${validFuelChains.size} valid closed fuel chains; fuel cost per 100 km = sum(amount) / sum(distanceKm) * 100.",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "Threshold requires at least 1 valid closed fuel chain.",
                emptyStateMessage = "Log a valid closed fuel chain to unlock fuel cost per 100 km.",
            )
        }

        val serviceSpendMetric: AnalyticsMetric<Double> = if (filteredServices.isNotEmpty()) {
            AnalyticsMetric(
                value = filteredServices.sumOf { it.totalAmount },
                trace = "${filteredServices.size} completed service records in range; service spend = sum(totalAmount).",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "No completed service records in range.",
                emptyStateMessage = "Add a completed service record to see service spend.",
            )
        }

        val trendMetric = buildTrendMetric(
            range = range,
            vehicleIds = vehicleIds,
            mileageEntriesByVehicle = filteredMileageByVehicle,
            fuelEntriesByVehicle = fuelEntriesByVehicle,
            expenseEntriesByVehicle = expenseEntriesByVehicle,
            serviceEntriesByVehicle = serviceEntriesByVehicle,
        )

        return AnalyticsDashboardSummary(
            scopeMode = scopeMode,
            range = range,
            selectedVehicleCount = vehicleIds.size,
            totalExpenses = totalExpensesMetric,
            categoryBreakdown = categoryBreakdownMetric,
            averageCheck = averageCheckMetric,
            totalDistanceKm = totalDistanceMetric,
            verifiedMileageRatioPercent = verifiedRatioMetric,
            overallTrustScore = overallTrustMetric,
            costPerKm = costPerKmMetric,
            fuelConsumptionLPer100Km = fuelConsumptionMetric,
            fuelCostPer100Km = fuelCostMetric,
            serviceSpend = serviceSpendMetric,
            trend = trendMetric,
            validFuelChains = validFuelChains.size,
        )
    }

    private fun buildTrendMetric(
        range: AnalyticsTimeRange,
        vehicleIds: List<String>,
        mileageEntriesByVehicle: Map<String, List<MileageEntrySummary>>,
        fuelEntriesByVehicle: Map<String, List<FuelEntrySummary>>,
        expenseEntriesByVehicle: Map<String, List<ExpenseEntrySummary>>,
        serviceEntriesByVehicle: Map<String, List<ServiceEntrySummary>>,
    ): AnalyticsMetric<List<AnalyticsTrendPoint>> {
        val bucketStarts = bucketStarts(range)
        val points = bucketStarts.map { bucketStart ->
            val bucketEnd = nextBucketStart(bucketStart, range)
            val bucketExpenses = vehicleIds.sumOf { vehicleId ->
                expenseEntriesByVehicle[vehicleId].orEmpty()
                    .filter { it.timestampEpochMillis in bucketStart until bucketEnd }
                    .sumOf { it.totalAmount }
            }
            val bucketFuelAmount = vehicleIds.sumOf { vehicleId ->
                fuelEntriesByVehicle[vehicleId].orEmpty()
                    .filter { it.timestampEpochMillis in bucketStart until bucketEnd }
                    .sumOf { it.totalAmount }
            }
            val bucketServices = vehicleIds.sumOf { vehicleId ->
                serviceEntriesByVehicle[vehicleId].orEmpty()
                    .filter { it.timestampEpochMillis in bucketStart until bucketEnd }
                    .sumOf { it.totalAmount }
            }
            val bucketDistance = vehicleIds.sumOf { vehicleId ->
                vehicleDistanceKm(
                    mileageEntriesByVehicle[vehicleId].orEmpty()
                        .filter { it.timestampEpochMillis in bucketStart until bucketEnd },
                ) ?: 0.0
            }
            AnalyticsTrendPoint(
                bucketStartEpochMillis = bucketStart,
                label = bucketLabel(bucketStart, range),
                combinedSpendAmount = bucketExpenses + bucketFuelAmount + bucketServices,
                distanceKm = bucketDistance,
            )
        }

        val populatedBuckets = points.count { it.combinedSpendAmount > 0.0 || it.distanceKm > 0.0 }
        return if (populatedBuckets >= 2) {
            AnalyticsMetric(
                value = points,
                trace = "$populatedBuckets populated time buckets; trend chart enabled because at least 2 buckets contain data.",
            )
        } else {
            AnalyticsMetric(
                value = null,
                trace = "Trend charts require at least 2 time buckets containing data.",
                emptyStateMessage = "Log data across more than one time bucket to unlock trends.",
            )
        }
    }

    private fun vehicleDistanceKm(entries: List<MileageEntrySummary>): Double? {
        val ordered = entries.sortedBy { it.timestampEpochMillis }
        if (ordered.size < 2) {
            return null
        }
        return (ordered.last().reading.km - ordered.first().reading.km).takeIf { it > 0.0 }
    }

    private fun bucketStarts(range: AnalyticsTimeRange): List<Long> {
        val startDate = Instant.ofEpochMilli(range.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val endDate = Instant.ofEpochMilli(range.endExclusiveEpochMillis - 1).atZone(ZoneId.systemDefault()).toLocalDate()
        val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1

        return if (totalDays <= 45) {
            generateSequence(startDate) { date ->
                date.plusDays(1).takeIf { !it.isAfter(endDate) }
            }.map { it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }.toList()
        } else {
            generateSequence(startDate.withDayOfMonth(1)) { date ->
                date.plusMonths(1).takeIf { !it.isAfter(endDate.withDayOfMonth(1)) }
            }.map { it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }.toList()
        }
    }

    private fun nextBucketStart(
        bucketStart: Long,
        range: AnalyticsTimeRange,
    ): Long {
        val bucketDate = Instant.ofEpochMilli(bucketStart).atZone(ZoneId.systemDefault()).toLocalDate()
        val rangeDays = ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(range.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
            Instant.ofEpochMilli(range.endExclusiveEpochMillis - 1).atZone(ZoneId.systemDefault()).toLocalDate(),
        ).toInt() + 1
        val nextDate = if (rangeDays <= 45) {
            bucketDate.plusDays(1)
        } else {
            bucketDate.plusMonths(1)
        }
        return minOf(
            nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            range.endExclusiveEpochMillis,
        )
    }

    private fun bucketLabel(
        bucketStart: Long,
        range: AnalyticsTimeRange,
    ): String {
        val bucketDate = Instant.ofEpochMilli(bucketStart).atZone(ZoneId.systemDefault()).toLocalDate()
        val rangeDays = ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(range.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate(),
            Instant.ofEpochMilli(range.endExclusiveEpochMillis - 1).atZone(ZoneId.systemDefault()).toLocalDate(),
        ).toInt() + 1
        return if (rangeDays <= 45) {
            bucketDate.dayOfMonth.toString().padStart(2, '0')
        } else {
            "${bucketDate.monthValue.toString().padStart(2, '0')}.${bucketDate.year}"
        }
    }
}
