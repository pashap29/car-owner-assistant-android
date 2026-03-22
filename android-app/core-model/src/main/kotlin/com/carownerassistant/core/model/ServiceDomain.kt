package com.carownerassistant.core.model

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed interface ServiceDraftValidationResult {
    data object Valid : ServiceDraftValidationResult
    data class Invalid(val message: String) : ServiceDraftValidationResult
}

object ServiceRules {
    fun totalAmount(
        workItems: List<ServiceWorkItem>,
        partItems: List<ServicePartItem>,
    ): Double {
        return workItems.sumOf { it.totalAmount } + partItems.sumOf { it.totalAmount }
    }

    fun validateDraft(
        title: String,
        workItems: List<ServiceWorkItem>,
        partItems: List<ServicePartItem>,
        mileageValue: Double?,
        mileageUnit: DistanceUnit?,
    ): ServiceDraftValidationResult {
        if (title.isBlank()) {
            return ServiceDraftValidationResult.Invalid("Service title is required.")
        }

        if (workItems.isEmpty() && partItems.isEmpty()) {
            return ServiceDraftValidationResult.Invalid("Add at least one work item or part item.")
        }

        if (workItems.any { it.title.isBlank() || !it.totalAmount.isFinite() || it.totalAmount <= 0.0 }) {
            return ServiceDraftValidationResult.Invalid("Each work item needs a title and positive amount.")
        }

        if (partItems.any { it.title.isBlank() || it.quantity <= 0 || !it.totalAmount.isFinite() || it.totalAmount <= 0.0 }) {
            return ServiceDraftValidationResult.Invalid("Each part item needs a title, positive quantity, and positive amount.")
        }

        if ((mileageValue == null) != (mileageUnit == null)) {
            return ServiceDraftValidationResult.Invalid("Mileage value and unit must be provided together.")
        }

        if (mileageValue != null && mileageValue <= 0.0) {
            return ServiceDraftValidationResult.Invalid("Mileage must be greater than zero when provided.")
        }

        return ServiceDraftValidationResult.Valid
    }

    fun buildLinkedMileageDraft(draft: ServiceEntryDraft): MileageEntryDraft? {
        val value = draft.mileageValue ?: return null
        val unit = draft.mileageUnit ?: return null
        return MileageEntryDraft(
            vehicleId = draft.vehicleId,
            timestampEpochMillis = draft.timestampEpochMillis,
            value = value,
            inputUnit = unit,
            photoFilePath = null,
            origin = MileageEntryOrigin.SERVICE_CONTEXT,
            manualEntry = true,
        )
    }

    fun dialUri(phone: String?): String? {
        val normalized = phone
            ?.filter { it.isDigit() || it == '+' }
            ?.trim()
            .orEmpty()
        return if (normalized.isBlank()) null else "tel:$normalized"
    }

    fun mapsUri(address: String?): String? {
        val normalized = address?.trim().orEmpty()
        return if (normalized.isBlank()) {
            null
        } else {
            "geo:0,0?q=${URLEncoder.encode(normalized, StandardCharsets.UTF_8)}"
        }
    }
}
