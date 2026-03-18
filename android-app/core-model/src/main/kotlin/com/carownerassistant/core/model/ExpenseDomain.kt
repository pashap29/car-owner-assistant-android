package com.carownerassistant.core.model

sealed interface ExpenseDraftValidationResult {
    data object Valid : ExpenseDraftValidationResult
    data class Invalid(val message: String) : ExpenseDraftValidationResult
}

object ExpenseRules {
    fun validateDraft(
        amount: Double?,
        mileageValue: Double?,
        mileageUnit: DistanceUnit?,
    ): ExpenseDraftValidationResult {
        if (amount == null || amount <= 0.0) {
            return ExpenseDraftValidationResult.Invalid("Amount must be greater than zero.")
        }

        if ((mileageValue == null) != (mileageUnit == null)) {
            return ExpenseDraftValidationResult.Invalid("Mileage value and unit must be provided together.")
        }

        if (mileageValue != null && mileageValue <= 0.0) {
            return ExpenseDraftValidationResult.Invalid("Mileage value must be greater than zero when provided.")
        }

        return ExpenseDraftValidationResult.Valid
    }

    fun applyFilter(
        entries: List<ExpenseEntrySummary>,
        filter: ExpenseFilter,
    ): List<ExpenseEntrySummary> {
        val normalizedQuery = filter.query.trim().lowercase()

        return entries
            .filter { entry ->
                filter.category == null || entry.category == filter.category
            }
            .filter { entry ->
                if (normalizedQuery.isBlank()) {
                    true
                } else {
                    searchableText(entry).contains(normalizedQuery)
                }
            }
            .sortedByDescending { it.timestampEpochMillis }
    }

    fun searchableText(entry: ExpenseEntrySummary): String {
        return buildString {
            append(entry.category.name.lowercase())
            append(' ')
            append(entry.note.lowercase())
        }.trim()
    }
}
