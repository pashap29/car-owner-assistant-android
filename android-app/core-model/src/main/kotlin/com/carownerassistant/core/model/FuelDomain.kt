package com.carownerassistant.core.model

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface FuelDraftValidationResult {
    data object Valid : FuelDraftValidationResult
    data class Invalid(val message: String) : FuelDraftValidationResult
}

object FuelQrParser {
    fun parse(rawPayload: String): FuelQrPrefill {
        val normalized = rawPayload.trim()
        if (normalized.isBlank()) {
            return FuelQrPrefill(
                timestampEpochMillis = null,
                totalAmount = null,
            )
        }

        val values = normalized
            .split('&')
            .mapNotNull { token ->
                val parts = token.split('=', limit = 2)
                if (parts.size != 2) {
                    null
                } else {
                    parts[0].trim().lowercase() to decodeToken(parts[1].trim())
                }
            }
            .toMap()

        val timestamp = sequenceOf("t", "date", "datetime")
            .mapNotNull { key -> values[key] }
            .mapNotNull(::parseTimestamp)
            .firstOrNull()

        val totalAmount = sequenceOf("s", "sum", "total")
            .mapNotNull { key -> values[key] }
            .mapNotNull(::parseAmount)
            .firstOrNull()

        return FuelQrPrefill(
            timestampEpochMillis = timestamp,
            totalAmount = totalAmount,
        )
    }

    private fun decodeToken(value: String): String {
        return runCatching {
            URLDecoder.decode(value, StandardCharsets.UTF_8)
        }.getOrDefault(value)
    }

    private fun parseAmount(value: String): Double? {
        return value.replace(',', '.').toDoubleOrNull()
    }

    private fun parseTimestamp(value: String): Long? {
        val formatters = listOf(
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"),
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmm"),
        )

        formatters.forEach { formatter ->
            val parsed = runCatching {
                LocalDateTime.parse(value, formatter)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()

            if (parsed != null) {
                return parsed
            }
        }

        return runCatching {
            LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }
}

object FuelRules {
    fun validateDraft(
        liters: Double?,
        totalAmount: Double?,
        odometerValue: Double?,
        odometerUnit: DistanceUnit?,
    ): FuelDraftValidationResult {
        if (liters == null || liters <= 0.0) {
            return FuelDraftValidationResult.Invalid("Liters must be greater than zero.")
        }

        if (totalAmount == null || totalAmount <= 0.0) {
            return FuelDraftValidationResult.Invalid("Total amount must be greater than zero.")
        }

        if (odometerValue == null || odometerValue <= 0.0) {
            return FuelDraftValidationResult.Invalid("Enter the new odometer value for fuel logging.")
        }

        if (odometerUnit == null) {
            return FuelDraftValidationResult.Invalid("Select the odometer unit.")
        }

        return FuelDraftValidationResult.Valid
    }

    fun suggestFuelType(entries: List<FuelEntrySummary>): FuelType? {
        return entries
            .groupBy { it.fuelType }
            .map { (fuelType, groupedEntries) ->
                Triple(
                    fuelType,
                    groupedEntries.size,
                    groupedEntries.maxOfOrNull { it.timestampEpochMillis } ?: Long.MIN_VALUE,
                )
            }
            .sortedWith(
                compareByDescending<Triple<FuelType, Int, Long>> { it.second }
                    .thenByDescending { it.third },
            )
            .firstOrNull()
            ?.first
    }

    fun computeMileageDeltaKm(
        previousMileageKm: Double?,
        newReading: OdometerReading,
    ): Double? {
        return previousMileageKm?.let { newReading.km - it }
    }

    fun buildLinkedMileageDraft(draft: FuelEntryDraft): MileageEntryDraft {
        val value = requireNotNull(draft.odometerValue) {
            "Fuel mileage linkage requires an odometer value."
        }
        val unit = requireNotNull(draft.odometerUnit) {
            "Fuel mileage linkage requires an odometer unit."
        }
        return MileageEntryDraft(
            vehicleId = draft.vehicleId,
            timestampEpochMillis = draft.timestampEpochMillis,
            value = value,
            inputUnit = unit,
            photoFilePath = null,
            origin = MileageEntryOrigin.FUEL_CONTEXT,
            manualEntry = true,
        )
    }
}
