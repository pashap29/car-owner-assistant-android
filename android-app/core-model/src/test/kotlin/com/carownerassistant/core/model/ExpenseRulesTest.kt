package com.carownerassistant.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseRulesTest {

    @Test
    fun `invalid amount is rejected`() {
        val result = ExpenseRules.validateDraft(
            amount = 0.0,
            mileageValue = null,
            mileageUnit = null,
        )

        assertEquals(
            ExpenseDraftValidationResult.Invalid("Amount must be greater than zero."),
            result,
        )
    }

    @Test
    fun `mileage requires both value and unit`() {
        val result = ExpenseRules.validateDraft(
            amount = 100.0,
            mileageValue = 1000.0,
            mileageUnit = null,
        )

        assertEquals(
            ExpenseDraftValidationResult.Invalid("Mileage value and unit must be provided together."),
            result,
        )
    }

    @Test
    fun `filter applies category and query`() {
        val filtered = ExpenseRules.applyFilter(
            entries = listOf(
                expense(id = "1", category = ExpenseCategory.PARKING, note = "Airport parking"),
                expense(id = "2", category = ExpenseCategory.INSURANCE, note = "Policy renewal"),
                expense(id = "3", category = ExpenseCategory.PARKING, note = "Mall"),
            ),
            filter = ExpenseFilter(
                query = "air",
                category = ExpenseCategory.PARKING,
            ),
        )

        assertEquals(listOf("1"), filtered.map { it.id })
    }

    @Test
    fun `blank query returns all matching category entries sorted desc`() {
        val filtered = ExpenseRules.applyFilter(
            entries = listOf(
                expense(id = "1", category = ExpenseCategory.OTHER, note = "A", timestamp = 1L),
                expense(id = "2", category = ExpenseCategory.OTHER, note = "B", timestamp = 5L),
            ),
            filter = ExpenseFilter(category = ExpenseCategory.OTHER),
        )

        assertEquals(listOf("2", "1"), filtered.map { it.id })
    }

    @Test
    fun `valid draft passes`() {
        val result = ExpenseRules.validateDraft(
            amount = 149.0,
            mileageValue = 12000.0,
            mileageUnit = DistanceUnit.KM,
        )

        assertTrue(result is ExpenseDraftValidationResult.Valid)
    }

    private fun expense(
        id: String,
        category: ExpenseCategory,
        note: String,
        timestamp: Long = 0L,
    ): ExpenseEntrySummary {
        return ExpenseEntrySummary(
            id = id,
            vehicleId = "vehicle-1",
            timestampEpochMillis = timestamp,
            category = category,
            totalAmount = 100.0,
            note = note,
            attachmentPath = null,
            mileageReading = null,
        )
    }
}
