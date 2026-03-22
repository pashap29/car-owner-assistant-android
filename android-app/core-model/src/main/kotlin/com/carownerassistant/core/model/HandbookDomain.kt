package com.carownerassistant.core.model

object HandbookRules {
    private val defaults = listOf(
        HandbookSectionSummary(
            id = HandbookSectionId.IDENTIFICATION,
            title = "Identification",
            content = "",
        ),
        HandbookSectionSummary(
            id = HandbookSectionId.INSURANCE,
            title = "Insurance",
            content = "",
        ),
        HandbookSectionSummary(
            id = HandbookSectionId.MAINTENANCE_NOTES,
            title = "Maintenance Notes",
            content = "",
        ),
        HandbookSectionSummary(
            id = HandbookSectionId.TIRE_INFO,
            title = "Tire Info",
            content = "",
        ),
        HandbookSectionSummary(
            id = HandbookSectionId.EMERGENCY_CONTACTS,
            title = "Emergency Contacts",
            content = "",
        ),
        HandbookSectionSummary(
            id = HandbookSectionId.GENERAL_NOTES,
            title = "General Notes",
            content = "",
        ),
    )

    fun defaultSections(): List<HandbookSectionSummary> = defaults

    fun sanitizeVin(input: String): String {
        return input
            .trim()
            .uppercase()
            .take(17)
    }

    fun mergeSections(
        storedSections: List<HandbookSectionSummary>,
    ): List<HandbookSectionSummary> {
        val storedById = storedSections.associateBy { it.id }
        return defaults.map { default ->
            storedById[default.id]?.copy(title = default.title) ?: default
        }
    }

    fun sortDocuments(
        documents: List<VehicleDocumentSummary>,
    ): List<VehicleDocumentSummary> {
        return documents.sortedWith(
            compareByDescending<VehicleDocumentSummary> { it.createdAtEpochMillis }
                .thenBy { it.displayName.lowercase() },
        )
    }
}
