package com.carownerassistant.feature.vehicle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleDraftValidatorTest {

    @Test
    fun `blank display name is invalid`() {
        val result = VehicleDraftValidator.validate(
            displayName = " ",
            yearText = "",
        )

        assertEquals(
            VehicleDraftValidationResult.Invalid("Display name is required."),
            result,
        )
    }

    @Test
    fun `non numeric year is invalid`() {
        val result = VehicleDraftValidator.validate(
            displayName = "Family car",
            yearText = "twenty",
        )

        assertEquals(
            VehicleDraftValidationResult.Invalid("Year must be a number."),
            result,
        )
    }

    @Test
    fun `year outside supported range is invalid`() {
        val result = VehicleDraftValidator.validate(
            displayName = "Family car",
            yearText = "1899",
        )

        assertEquals(
            VehicleDraftValidationResult.Invalid("Year must be between 1900 and 2100."),
            result,
        )
    }

    @Test
    fun `valid input passes validation`() {
        val result = VehicleDraftValidator.validate(
            displayName = "Family car",
            yearText = "2021",
        )

        assertTrue(result is VehicleDraftValidationResult.Valid)
    }
}
