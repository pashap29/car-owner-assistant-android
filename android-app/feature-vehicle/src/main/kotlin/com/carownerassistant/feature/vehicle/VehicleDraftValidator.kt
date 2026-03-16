package com.carownerassistant.feature.vehicle

import com.carownerassistant.core.model.VehicleDraft

sealed interface VehicleDraftValidationResult {
    data object Valid : VehicleDraftValidationResult
    data class Invalid(val message: String) : VehicleDraftValidationResult
}

object VehicleDraftValidator {
    fun validate(
        displayName: String,
        yearText: String,
    ): VehicleDraftValidationResult {
        if (displayName.isBlank()) {
            return VehicleDraftValidationResult.Invalid("Display name is required.")
        }

        if (yearText.isNotBlank()) {
            val year = yearText.toIntOrNull()
                ?: return VehicleDraftValidationResult.Invalid("Year must be a number.")

            if (year !in 1900..2100) {
                return VehicleDraftValidationResult.Invalid("Year must be between 1900 and 2100.")
            }
        }

        return VehicleDraftValidationResult.Valid
    }

    fun toDraft(state: VehicleEditorState): VehicleDraft {
        return VehicleDraft(
            displayName = state.displayName.trim(),
            make = state.make.trim(),
            model = state.model.trim(),
            year = state.year.trim().takeIf { it.isNotEmpty() }?.toInt(),
            plateNumber = state.plateNumber.trim(),
            makeActive = state.makeActive,
        )
    }
}
